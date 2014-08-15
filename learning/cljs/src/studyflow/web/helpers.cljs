(ns studyflow.web.helpers
  (:require [om.dom :as dom]
            [clojure.string :as string]))

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
                                          :className (fnil (partial str "secundary_action ") "")))
                             (update-js primary-button
                                        :className (partial str "btn green primary "))))))

(defn split-text-and-inputs [text inputs]
  (reduce
   (fn [pieces input]
     (loop [[p & ps] pieces
            out []]
       (if-not p
         out
         (if (gstring/contains p input)
           (let [[before & after] (string/split p (re-pattern input))]
             (-> out
                 (into [before input])
                 (into after)
                 (into ps)))
           (recur ps (conj out p))))))
   [text]
   inputs))
