(ns prsc.utils
  "utils functions"
)

(defn safenth [v idx]
  (if (and  (seqable? v) (integer? idx)  (< idx (count v)) )
    (nth v idx)
    nil
  ))

(defn call [fnname & args]
  (apply (resolve (symbol fnname )) args))
