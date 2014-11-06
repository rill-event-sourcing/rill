(ns studyflow.web.helpers
  (:require [om.core :as om]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [goog.events :as gevents]
            [goog.string :as gstring]
            [clojure.string :as string]
            [studyflow.web.history :refer [history-link]]))

(defn raw-html
  [raw]
  (dom/span #js {:dangerouslySetInnerHTML #js {:__html raw}} nil))

(defn update-js [js-obj key f]
  (let [key (if (keyword? key)
              (name key)
              key)
        p (.-props js-obj)]
    (aset p key (f (get p key)))
    js-obj))

(defn modal [content primary-button & [secondary-button]]
  (dom/div #js {:id "m-modal"
                :className "show"}
           (dom/div #js {:className "modal_inner"}
                    content
                    (dom/div #js {:className "modal_footer"}
                             (when secondary-button
                               (update-js secondary-button
                                          :className (fnil (partial str "btn big gray") "")))
                             (update-js primary-button
                                        :className (partial str "btn big yellow pull-right"))))))

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

(defn section-explanation-link [cursor chapter section]
  (-> (get-in cursor [:view :selected-path])
      (assoc :chapter-id (:id chapter)
             :section-id (:id section)
             :section-tab :explanation
             :main :learning)
      history-link))

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
