(ns studyflow.loop-tools)

(defmacro while-let
  [[test-binding test-expression] & body]
  `(loop []
     (when-let [~test-binding ~test-expression]
       ~@body
       (recur))))


