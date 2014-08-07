(ns studyflow.learning.course-material
  "This is the hierarchical, normalized model for the course material"
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [schema.core :as s]
            [schema.coerce :as coerce]
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
   :prefix s/Str
   :suffix s/Str
   :width s/Int
   :correct-answers  #{s/Str}})

(def SectionQuestion
  {:id Id
   :text RichText
   :worked-out-answer RichText
   :line-input-fields [LineInputField]
   :multiple-choice-input-fields [MultipleChoiceInputField]})

(def SubSection
  {:id Id
   :title PlainText
   :text RichText})

(def Section
  {:id Id
   :title PlainText
   :subsections [SubSection]
   :questions #{SectionQuestion}})

(def Chapter
  {:id Id
   :title PlainText
   :sections [Section]})

(def CourseMaterial
  {:id Id
   :name PlainText
   :chapters [Chapter]})

(def parse-course-material*
  (coerce/coercer CourseMaterial schema-tools/schema-coercion-matcher))

(defn question-text-to-tree [question]
  (let [input-names (-> #{}
                        (into (map :name (:line-input-fields question)))
                        (into (map :name (:multiple-choice-input-fields question))))
        ;; turn _INPUT_1_ into a html tag
        question-text (:text question)
        question-text (reduce
                       (fn [qt input-name]
                         (string/replace qt input-name
                                         (str "<input name=\"" input-name "\"/>")))
                       question-text
                       input-names)
        ;; html-snippet doesn't add html/body, need our own root
        tags {:tag :div
              :attrs nil
              :content (html/html-snippet question-text)}]
    tags))

(defn transform-question-text-to-tree [course-material]
  (walk/prewalk
   (fn [node]
     (if (map? node)
       (if-let [qs-node (get node :questions)]
         ;; super defensive in case :questions appear anywhere else
         (if (every? (fn [q-node]
                       (and (contains? q-node :id)
                            (contains? q-node :text)
                            (contains? q-node :worked-out-answer)
                            (contains? q-node :line-input-fields)
                            (contains? q-node :multiple-choice-input-fields)))
                     qs-node)
           (update-in node [:questions]
                      (fn [qs]
                        (mapv #(assoc % :tag-tree
                                      (question-text-to-tree %)) qs)))
           node)
         node)
       node))
   course-material))

(def parse-course-material
  (comp
   transform-question-text-to-tree
   (schema-tools/strict-coercer parse-course-material*)))



