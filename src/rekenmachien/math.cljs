(ns rekenmachien.math)

(def pi (.-PI js/Math))

(def rad->deg #(/ % (/ pi 180)))
(def deg->rad #(* % (/ pi 180)))

(defn gonio [f]
  (fn [v]
    (let [r (f (deg->rad v))]
      (cond (< -1e-15 r 1e-15) 0
            (> r 1e16) Infinity
            :else r))))

(defn inv-gonio [f]
  (fn [v]
    (rad->deg (f v))))

(def add +)
(def sub -)
(def mul *)
(def div /)
(def neg #(* % -1))
(def x1 #(.pow js/Math %1 -1))
(def x2 #(.pow js/Math %1 2))
(def pow #(.pow js/Math %1 %2))
(def x10y #(* %1 (.pow js/Math 10 %2)))
(def sin (gonio (.-sin js/Math)))
(def cos (gonio (.-cos js/Math)))
(def tan (gonio (.-tan js/Math)))
(def asin (inv-gonio (.-asin js/Math)))
(def acos (inv-gonio (.-acos js/Math)))
(def atan (inv-gonio (.-atan js/Math)))
(def sqrt #(.sqrt js/Math %))
