(ns rekenmachien.math)

(declare Frac)

(defn finite? [val]
  (not (or (.isNaN js/window val)
           (= js/Infinity val)
           (= (* -1 js/Infinity) val))))

(defn fraction? [val]
  (instance? Frac val))

(defn float? [val]
  (and (number? val)
       (> (.indexOf (str val) ".") -1)))

(defn abs [val]
  (.abs js/Math val))

(deftype Frac [n d]
  Object

  (add [_ x]
    (cond
     (fraction? x) (Frac. (+ (* n (.-d x)) (* d (.-n x))) (* d (.-d x)))
     (integer? x) (Frac. (+ n (* x d)) d)
     (float? x) (+ (/ n d) x)))

  (sub [this x]
    (.add this (if (fraction? x) (.negate x) (* -1 x))))

  (mul [_ x]
    (cond
     (fraction? x) (Frac. (* n (.-n x)) (* d (.-d x)))
     (integer? x) (Frac. (* n x) d)
     (float? x) (* (/ n d) x)))

  (div [_ x]
    (cond
     (fraction? x) (Frac. (* n (.-d x)) (* d (.-n x)))
     (integer? x) (Frac. n (* d x))
     (float? x) (/ (/ n d) x)))

  (pow [_ x]
    (let [x (if (fraction? x) (.decimal x) x)]
      (cond
       (integer? x) (let [[n d x] (if (< x 0) [d n (abs x)] [n d x])]
                      (Frac. (.pow js/Math n x) (.pow js/Math d x)))
       (float? x) (.pow js/Math (/ n d) x))))

  (decimal [_]
    (/ n d))

  (negate [_]
    (Frac. (* -1 n) d))

  (gcf [_]
    (loop [n (abs n) d (abs d)]
      (let [r (rem n d)]
        (when-not (finite? r) (throw :math-error))
        (if (= r 0) d (recur d r)))))

  (toString [this]
    (let [s (if (or (< n 0) (< d 0)) "-")
          f (.gcf this)
          n (/ (abs n) f)
          d (/ (abs d) f)]
      (str s n (when-not (= d 1) (str "/" d)))))

  IEquiv
  (-equiv [this x]
    (and (fraction? x) (= (.toString this) (.toString x)))))

(def pi (.-PI js/Math))

(defn decimal [x]
  (if (fraction? x) (.decimal x) x))

(def rad->deg #(/ (decimal %) (/ pi 180)))
(def deg->rad #(* (decimal %) (/ pi 180)))

(defn- gonio [f]
  (fn [v]
    (let [r (f (deg->rad v))]
      (cond (< -1e-15 r 1e-15) 0
            (> r 1e16) Infinity
            :else r))))

(defn- inv-gonio [f]
  (fn [v]
    (rad->deg (f v))))

(defn- with-frac [n-fn f-fn]
  (fn [x y]
    (cond
     (fraction? x) (f-fn x y)
     (fraction? y) (if (integer? x) (f-fn (Frac. x 1) y) (n-fn x (.decimal y)))
     :else (n-fn x y))))

(def add (with-frac + #(.add %1 %2)))
(def sub (with-frac - #(.sub %1 %2)))
(def mul (with-frac * #(.mul %1 %2)))
(def div (with-frac / #(.div %1 %2)))
(def neg #(mul % -1))
(def x1 (with-frac #(.pow js/Math %1 -1) #(.pow %1 -1)))
(def x2 (with-frac #(.pow js/Math %1 2) #(.pow %1 2)))
(def pow (with-frac #(.pow js/Math %1 %2) #(.pow %1 %2)))
(def x10y (with-frac #(* %1 (.pow js/Math 10 %2)) #(.mul %1 (.pow js/Math 10 (decimal %2)))))
(def sin (gonio (.-sin js/Math)))
(def cos (gonio (.-cos js/Math)))
(def tan (gonio (.-tan js/Math)))
(def asin (inv-gonio (.-asin js/Math)))
(def acos (inv-gonio (.-acos js/Math)))
(def atan (inv-gonio (.-atan js/Math)))
(def sqrt #(.sqrt js/Math %))

(defn frac [x y]
  (cond
   (fraction? x)
   (if (.-done x)
     (throw :syntax-error)
     (let [v (Frac. (+ (mul (.-n x) y) (.-d x)) y)]
       (set! (.-done v) true)
       v))
   (and (integer? x) (integer? y)) (Frac. x y)
   (and (number? x) (number? y)) (/ x y)))
