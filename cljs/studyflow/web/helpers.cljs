(ns studyflow.web.helpers
  (:require [om.core :as om]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [goog.events :as gevents]
            [goog.string :as gstring]
            [clojure.string :as string]
            [studyflow.web.history :refer [path-url]]))


(defn raw-html
  [raw]
  (dom/span #js {:dangerouslySetInnerHTML #js {:__html raw}} nil))

(defn on-enter
  "Execute f and prevent default actions when enter key event is passed"
  [f]
  (fn [e]
    (if (= (.-keyCode e) 13) ;; ENTER key
      (do (.stopPropagation e)
          (.preventDefault e)
          (f)
          false)
      true)))

(defn- modal-fn
  [cursor owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "m-modal"
                    :className "show"
                    :onKeyPress (on-enter (:submit-fn cursor))}
               (dom/div #js {:className "modal_inner"}
                        (:content cursor)
                        (dom/div #js {:className "modal_footer"}
                                 (:secondary-button cursor)
                                 (dom/button #js {:className  "btn big yellow pull-right"
                                                  :onClick (:submit-fn cursor)
                                                  :ref "PRIMARY_BUTTON"}
                                             (:submit-text cursor))))))
    om/IDidMount
    (did-mount [_]
      (when-let [focusing (om/get-node owner "PRIMARY_BUTTON")]
        (.focus focusing)))))

(defn modal [content submit-text submit-fn & [secondary-button]]
  (om/build modal-fn {:content content
                      :submit-fn submit-fn
                      :submit-text submit-text
                      :secondary-button secondary-button}))

(def html->om
  {"a" dom/a, "b" dom/b, "big" dom/big, "br" dom/br, "dd" dom/dd, "div" dom/div,
   "dl" dom/dl, "dt" dom/dt, "em" dom/em, "fieldset" dom/fieldset,
   "h1" dom/h1, "h2" dom/h2, "h3" dom/h3, "h4" dom/h4, "h5" dom/h5, "h6" dom/h6,
   "hr" dom/hr, "i" dom/i, "img" dom/img, "li" dom/li, "ol" dom/ol, "pre" dom/pre,
   "q" dom/q,"s" dom/s,"small" dom/small, "span" dom/span, "strong" dom/strong,
   "sub" dom/sub, "sup" dom/sup, "table" dom/table, "tbody" dom/tbody, "td" dom/td,
   "tfoot" dom/tfoor, "th" dom/th, "thead" dom/thead, "tr" dom/tr, "u" dom/u,
   "ul" dom/ul})

(defn attrs->js-obj [attrs]
  (let [jo (js-obj)]
    (when-let [className (:class attrs)]
      (aset jo (name :className) className))
    (when-let [src (:src attrs)]
      (aset jo (name :src) src))
    (when-let [id (:id attrs)]
      (aset jo (name :id) id))
    (when-let [style (:style attrs)]
      (aset jo (name :style) (apply js-obj (mapcat (fn [[k v]] [(name k) v]) style))))
    (when-let [width (:width attrs)]
      (aset jo (name :width) width))
    (when-let [height (:height attrs)]
      (aset jo (name :height) height))
    jo))

(defn tag-tree-to-om [tag-tree inputs]
  (let [tag-tree (om/value tag-tree)
        descent (fn descent [tag-tree]
                  (cond
                   (and (map? tag-tree)
                        (contains? tag-tree :tag)
                        (contains? tag-tree :attrs)
                        (contains? tag-tree :content))
                   (let [{:keys [tag attrs content] :as node} tag-tree]
                     (if (= tag "img")
                       (dom/img (if-let [style (:style attrs)]
                                  #js {:src (:src attrs)
                                       :style (apply js-obj (mapcat (fn [[k v]] [(name k) v]) style))}
                                  (if (and (:width attrs)
                                           (:height attrs))
                                    #js {:src (:src attrs)
                                         :width (:width attrs)
                                         :height (:height attrs)}
                                    (if-let [width (:width attrs)]
                                      #js {:src (:src attrs)
                                           :width width}
                                      (if-let [height (:height attrs)]
                                        #js {:src (:src attrs)
                                             :height height}
                                        #js {:src (:src attrs)})))))
                       (if (= tag "div")
                         (apply dom/div #js {:className (:class attrs)}
                                (map descent content))
                         (if (= tag "span")
                           (apply dom/span #js {:className (:class attrs)}
                                  (map descent content))
                           (if (= tag "ul")
                             (apply dom/ul #js {:className (:class attrs)}
                                    (map descent content))
                             (if (= tag "li")
                               (apply dom/li #js {:className (:class attrs)}
                                      (map descent content))
                               (if (= tag "u")
                                 (apply dom/u nil
                                        (map descent content))
                                 (if (= tag "br")
                                   (dom/br nil)
                                   (if (= tag "table")
                                     (apply dom/table #js {:className (:class attrs)}
                                            (map descent content))
                                     (if (= tag "tr")
                                       (apply dom/tr #js {:className (:class attrs)}
                                              (map descent content))
                                       (if (= tag "td")
                                         (apply dom/td #js {:className (:class attrs)}
                                                (map descent content))
                                         (if-let [build-fn (get html->om tag)]
                                           (apply build-fn
                                                  ;;(attrs->js-obj
                                                  ;;attrs)
                                                  #js {:className (get attrs :class "")}
                                                  (map descent content))
                                           (cond
                                            (= tag "input")
                                            (get inputs (:name attrs))
                                            (= tag "svg")
                                            (raw-html content)
                                            (= tag "iframe")
                                            (raw-html content)
                                            :else
                                            (apply dom/span #js {:className "default-html-to-om"}
                                                   (map descent content)))))))))))))))
                   (string? tag-tree)
                   tag-tree))]
    (descent tag-tree)))

(defn some-input-in-question-selected [refs]
  (let [active (.-activeElement js/document)]
    (some #{active}
          (-> (js->clj refs)
              (dissoc "FOCUSED_INPUT")
              vals
              (->>
               (map #(.getDOMNode %)))))))

(defn focus-input-box [owner]
  ;; we always call this, even when there's no element called
  ;; "FOCUSED_INPUT". om/get-node can't handle that case
  (when-let [refs (.-refs owner)]
    ;; need to set the focus on a non disabled field for firefox key handling
    (if-let [button-ref (aget refs "FOCUSED_BUTTON")]
      (when-let [button (.getDOMNode button-ref)]
        (.focus button))
      (when-let [input-ref (aget refs "FOCUSED_INPUT")]
        (when-let [input-field (.getDOMNode input-ref)]
          (when (and (= "" (.-value input-field))
                     (not (some-input-in-question-selected refs)))
            (.focus input-field)))))))

(defn section-explanation-url [cursor chapter section]
  (-> (get-in cursor [:view :selected-path])
      (assoc :chapter-id (:id chapter)
             :section-id (:id section)
             :section-tab :explanation
             :main :learning)
      path-url))

(def ipad? (js/navigator.userAgent.match #"iPhone|iPad|iPod"))

(defn ipad-scroll-on-inputs-blur-fix []
  ;; fixed header and side-nav move around in ipad upon focusing an
  ;; input box (and showing the keyboard), this unfocuses the input
  ;; when touching outside it, and that makes the ipad reset its layout
  (when ipad?
    (let [app (gdom/getElement "app")]
      (gevents/listen app
                      "touchstart"
                      (fn [e]
                        ;; without this you can't onclick the button
                        ;; in the tooltip anymore
                        (when (let [node-name (.. e -target -nodeName)]
                                (and (not= node-name "INPUT")
                                     (not= node-name "BUTTON")))
                          (.blur js/document.activeElement))
                        true)))))

(defn ipad-reset-header []
  (when ipad?
    (js/window.scrollTo js/document.body.-scrollLeft js/document.body.-scrollTop)))

(defn ipad-fix-scroll-after-switching []
  ;; when switching between explantion and questions, if you are
  ;; scrolled down a lot in explanation the question will be scrolled
  ;; of screen whn switching to questions
  (when ipad?
    (js/document.body.scrollIntoViewIfNeeded)))

(defn tool-box
  [tools]
  (let [tool-names {"pen_and_paper" "Pen & Papier"
                    "calculator" "Rekenmachine"}]
    (apply dom/div #js {:id "toolbox"}
           (map (fn [tool]
                  (dom/div #js {:className (str "tool " tool)}
                           (dom/div #js {:className "m-tooltip"} (get tool-names tool) )))
                tools))))

(defn input-builders
  [cursor question-id question-data current-answers enabled cursor-path]
  (-> {}
      (into (for [mc (:multiple-choice-input-fields question-data)]
              (let [input-name (:name mc)]
                [input-name
                 ;; WARNING using dom/ul & dom/li here breaks
                 (apply dom/span #js {:className "mc-list"}
                        (for [choice (map :value (:choices mc))]
                          (let [id (str input-name "-" choice)]
                            (dom/span #js {:className "mc-choice"}
                                      (dom/input #js {:id id
                                                      :react-key (str question-id "-" input-name "-" choice)
                                                      :type "radio"
                                                      :checked (= choice (get current-answers input-name))
                                                      :disabled (not enabled)
                                                      :onChange (when enabled (fn [event]
                                                                                (om/update!
                                                                                 cursor
                                                                                 (conj cursor-path input-name)
                                                                                 choice)))})
                                      (dom/label #js {:htmlFor id}
                                                 (raw-html choice))))))])))
      (into (for [[field ref] (map list
                                   (:line-input-fields question-data)
                                   (if enabled
                                     (into ["FOCUSED_INPUT"]
                                           (rest (map :name (:line-input-fields question-data))))
                                     (map :name (:line-input-fields question-data))))]
              (let [input-name (:name field)
                    input-classes (str ""
                                       (when (:prefix field) "has-prefix ")
                                       (when (:suffix field) "has-suffix "))
                    input-options (case (:style field)
                                    "small" {:class (str input-classes "small-input") :length 5}
                                    "exponent" {:class (str input-classes "exponent-input") :length 3}
                                    {:class (str input-classes "big-input")})]
                [input-name
                 (dom/span nil
                           (when-let [prefix (:prefix field)]
                             (dom/span #js {:className "prefix"} prefix))
                           (dom/input
                            #js {:className (:class input-options)
                                 :maxLength (:length input-options)
                                 :value (get current-answers input-name "")
                                 :react-key (str question-id "-" ref)
                                 :ref ref
                                 :disabled (not enabled)
                                 :onChange (when enabled (fn [event]
                                                           (om/update!
                                                            cursor
                                                            (conj cursor-path input-name)
                                                            (.. event -target -value))))})
                           (when-let [suffix (:suffix field)]
                             (dom/span #js {:className "suffix"} suffix)))])))))

(defn click-once-button [value onclick & {:keys [enabled className]
                                          :or {enabled true}}]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:enabled enabled})
      om/IRender
      (render [_]
        (dom/button #js {:className (str "btn blue pull-right" (when className (str " " className)))
                         :onClick
                         (fn [_]
                           (ipad-reset-header)
                           (onclick)
                           (om/set-state-nr! owner :enabled false))
                         :disabled (not (om/get-state owner :enabled))}
                    value)))))
