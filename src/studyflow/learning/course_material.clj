(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]
            [schema.coerce :as coerce]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [studyflow.schema-tools :as schema-tools]))

(def RichText s/Str)
(def PlainText s/Str)
(def Id s/Uuid)
(def FieldName s/Str)

(def Choice
  {:value s/Str
   :correct s/Bool})

(def MultipleChoiceInputField
  {:name FieldName
   :choices [Choice]})

(def LineInputField
  {:name FieldName
   :style s/Str
   (s/optional-key :prefix) s/Str
   (s/optional-key :suffix) s/Str
   (s/optional-key :width) s/Int
   :correct-answers  #{s/Str}})

(def Reflection
  {:name FieldName
   :content s/Str
   :answer s/Str})

(def ExtraAnswer
  {:name FieldName
   :title s/Str
   :content s/Str})

(def Tool
  (s/enum "pen_and_paper" "calculator"))

(def SectionQuestion
  {:id Id
   :text RichText
   :tools [Tool]
   :line-input-fields [LineInputField]
   :multiple-choice-input-fields [MultipleChoiceInputField]
   (s/optional-key :worked-out-answer) RichText})

(def SubSection
  {:id Id
   :title PlainText
   :text RichText})

(def Section
  {:id Id
   :title PlainText
   :subsections [SubSection]
   :meijerink-criteria #{s/Str}
   :domains #{s/Str}
   :line-input-fields #{LineInputField}
   :questions (s/both #{SectionQuestion}
                      (s/pred (fn [s] (seq s)) 'not-empty))
   (s/optional-key :reflections) [Reflection]
   (s/optional-key :extra-examples) [ExtraAnswer]})

(def ChapterQuizQuestion
  {:id Id
   :text RichText
   :tools [Tool]
   :line-input-fields [LineInputField]
   :multiple-choice-input-fields [MultipleChoiceInputField]})

(def ChapterQuizQuestionSet
  {:title PlainText
   :id Id
   :questions #{ChapterQuizQuestion}})

(def Chapter
  {:id Id
   :title PlainText
   :remedial s/Bool
   (s/optional-key :chapter-quiz) [ChapterQuizQuestionSet]
   :sections [Section]})

(def EntryQuizQuestion
  {:id Id
   :text RichText
   :tools [Tool]
   :line-input-fields [LineInputField]
   :multiple-choice-input-fields [MultipleChoiceInputField]})

(def EntryQuiz
  {:instructions RichText
   :feedback RichText
   :threshold s/Int
   :questions [EntryQuizQuestion]})

(def CourseMaterial
  {:id Id
   :name PlainText
   :entry-quiz EntryQuiz
   :chapters [Chapter]})

(defn split-style-string [s]
  (into {} (for [kv (string/split s #";")
                 :let [[k v] (string/split kv #":")
                       k (string/trim k)
                       v (string/trim v)]
                 :when (and k v)]
             [k v])))

(defn text-with-inputs-to-tree [text input-names]
  (let [;; make a mapping of _SVG_n_ to svg tags, svg tags have xhtml
        ;; things that enlive eats, will be put back in the final
        ;; structure as a leaf
        [text replacements]
        (loop [text text
               replacements {}]
          (let [open-idx (.indexOf text "<svg")]
            (if (= open-idx -1)
              [text replacements]
              (let [pre-text (subs text 0 open-idx)
                    match+post-text (subs text open-idx)
                    close-idx (+ (.indexOf match+post-text "</svg>" 0) 6)
                    match-text (subs match+post-text 0 close-idx)
                    post-text (subs match+post-text close-idx)
                    match-name (str "_SVG_" (count replacements) "_")]
                (recur (str pre-text match-name post-text)
                       (assoc replacements match-name match-text))))))
        ;; turn _INPUT_1_ & _SVG_1_ into a html tag
        text (reduce
              (fn [text input-name]
                (string/replace text
                                (re-pattern input-name)
                                (fn [match]
                                  (if (.startsWith match "_INPUT_")
                                    (str "<input name=\"" input-name "\"/>")
                                    (str "<svg name=\"" input-name "\"/>")))))
              text
              (into input-names
                    (keys replacements)))
        ;; html-snippet doesn't add html/body, need our own root
        tags {:tag :div
              :attrs nil
              :content (html/html-snippet text)}
        ;; put the original svg string as its content, to be rendered
        ;; as DangerousHtml with react
        ;; also splice up style strings, om needs those as a map
        ;; also create content for iframe to be used as raw-html
        ;; also replace p tags with divs, because nesting in p tags
        ;; makes browers reorder things, which breaks react
        tags (walk/prewalk
              (fn [node]
                (if (map? node)
                  (cond
                   (and (contains? node :tag)
                        (= (:tag node) :svg))
                   (assoc node :content (get replacements (:name (:attrs node))))
                   (and (contains? node :tag)
                        (= (:tag node) :iframe))
                   (assoc node
                     :content (str "<iframe "
                                   (apply str
                                          (string/join " "
                                                       (for [[k v] (:attrs node)]
                                                         (str (name k) "=\"" v "\""))))
                                   "></iframe>")
                     :attrs {})
                   (and (contains? node :tag)
                        (= (:tag node) :p))
                   (-> node
                       (assoc :tag :div)
                       (update-in [:attrs :class] str " div-p"))
                   (and (contains? node :tag)
                        (contains? node :attrs)
                        (contains? (:attrs node) :style))
                   (update-in node [:attrs :style] split-style-string)
                   :else node)
                  node))
              tags)]
    tags))

(defn transform-explanation-to-tree [material]
  (update-in material [:chapters]
             (fn [chapters]
               (mapv (fn [chapter]
                       (update-in chapter [:sections]
                                  (fn [sections]
                                    (mapv (fn [section]
                                            (let [line-input-field-names (:line-input-fields section)]
                                              (update-in section [:subsections]
                                                         (fn [subsections]
                                                           (mapv
                                                            (fn [subsection]
                                                              (assoc subsection
                                                                :tag-tree
                                                                (try
                                                                  (text-with-inputs-to-tree
                                                                   (:text subsection)
                                                                   (map :name line-input-field-names))
                                                                  (catch Exception e
                                                                    (throw (ex-info (str "Material tag-tree failure" (:title subsection))
                                                                                    {:material (:name material)
                                                                                     :chapter (select-keys chapter [:id :title])
                                                                                     :section (select-keys section [:id :title])
                                                                                     :subsection (select-keys subsection [:id :title])}
                                                                                    e))))))
                                                            subsections)))))
                                          sections)))) chapters))))

(defn transform-question-text-to-tree [material]
  (walk/prewalk
   (fn [node]
     (if (map? node)
       (if-let [qs-node (get node :questions)]
         ;; super defensive in case :questions appear anywhere else
         ;; covers entry-quiz and section-test
         (if (every? (fn [q-node]
                       (and (contains? q-node :id)
                            (contains? q-node :text)
                            (contains? q-node :line-input-fields)
                            (contains? q-node :multiple-choice-input-fields)))
                     qs-node)
           (update-in node [:questions]
                      (fn [qs]
                        (mapv (fn [question]
                                (assoc question :tag-tree
                                       (text-with-inputs-to-tree
                                        (:text question)
                                        (-> #{}
                                            (into (map :name (:line-input-fields question)))
                                            (into (map :name (:multiple-choice-input-fields question))))))) qs)))
           node)
         node)
       node))
   material))

(def parse-course-material*
  (coerce/coercer CourseMaterial schema-tools/schema-coercion-matcher))

(def parse-course-material
  (comp
   transform-explanation-to-tree
   transform-question-text-to-tree
   (schema-tools/strict-coercer parse-course-material*)))
