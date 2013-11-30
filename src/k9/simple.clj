(ns k9.simple
  (:use clojure.test)
  (:use clojure.core.matrix)
  (:use clojure.core.matrix.operators))


;;Neurons
;;  Input Hidden  Output
;;  A     1       C
;;  B     2       D
;;        3


;; Connection Strengths
;; Input to Hidden => [[A1 A2 A3] [B1 B2 B3]]
;; Hidden to Output => [[1C 1D] [2C 2D] [3C 3D]]


(def input-neurons [1 0])
(def input-hidden-strengths [ [0.12 0.2 0.13]
                              [0.01 0.02 0.03]])
(def hidden-neurons [0 0 0])
(def hidden-output-strengths [[0.15 0.16]
                              [0.02 0.03]
                              [0.01 0.02]])
(def output-neurons [0 0])

(def activation-fn (fn [x] (Math/tanh x)))
(def dactivation-fn (fn [y] (- 1.0 (* y y))))

;; Propogate the input

(defn layer-activation [inputs strengths]
  "forward propogate the input of a layer"
  (mapv activation-fn
      (mapv #(reduce + %)
       (* inputs (transpose strengths)))))

(testing "layer-activation-input"
  (is ( == [0.11942729853438588 0.197375320224904 0.12927258360605834]
           (layer-activation input-neurons input-hidden-strengths))))

(def new-hidden-neurons
  [0.11942729853438588 0.197375320224904 0.12927258360605834])

(testing "layer-activation-hidden"
  (is ( == [0.02315019005321053 0.027608061500083565]
           (layer-activation new-hidden-neurons hidden-output-strengths))))

(def new-output-neurons
  (layer-activation new-hidden-neurons hidden-output-strengths))


;; Calculate the errors
;; Desired output/ targets  will be the inverse of the input

;; Calculate the output layer first
; the error at the output neurons (Desired value – actual value) and multiplying it by the gradient of the sigmoid function

(def targets [0 1])
(* (mapv dactivation-fn new-output-neurons)
   (- targets new-output-neurons))

(defn output-deltas [targets outputs]
  "measures the delta errors for the output layer (Desired value – actual value) and multiplying it by the gradient of the activation function"
  (* (mapv dactivation-fn outputs)
     (- targets outputs)))

(testing "output-deltas"
  (is (== [-0.023137783141771645 0.9716507764442904]
          (output-deltas targets new-output-neurons) )))

(def odeltas (output-deltas targets new-output-neurons))

;; calc the errors for the hidden layers
;; for the hidden layer the error gradient for each hidden neuron is
;; the gradient of the activation function multiplied by the weighted
;; sum of the errors at the output layer originating from that neuron

(+ (* -0.02313 0.15) (* 0.9716 0.16))
(* (dactivation-fn 0.119) 0.1519)

(+ (* -0.02313 0.02) (* 0.9716 0.03))
(* (dactivation-fn 0.1973) 0.286854)

(+ (* -0.02313 0.01) (* 0.9716 0.02))
(* (dactivation-fn 0.129) 0.0192007)

odeltas
hidden-output-strengths
(* (mapv dactivation-fn new-hidden-neurons)
 (mapv #(reduce + %)
       (* odeltas hidden-output-strengths)))

(defn hlayer-deltas [odeltas neurons strengths]
  (* (mapv dactivation-fn neurons)
     (mapv #(reduce + %)
           (* odeltas strengths))))

(testing "hidden-deltas"
  (is (== [0.14982559238071416 0.027569216735265096 0.018880751432503236]
          (hlayer-deltas
             odeltas
             new-hidden-neurons
             hidden-output-strengths))))

(def hdeltas (hlayer-deltas
              odeltas
              new-hidden-neurons
              hidden-output-strengths))

;; Update the output weights
;; change = odelta * hidden value
;; weight = weight + learning-rate * change
(def learning-rate 0.2)
odeltas
(+ 0.15 (* 0.2 (* -0.021699 0.119)))
(+ 0.16 (* 0.2 (* 0.971659 0.119)))

(+ 0.02 (* 0.2 (* -0.021699 0.1973)))
(+ 0.03 (* 0.2 (* 0.971659 0.1973)))

(+ 0.01 (* 0.2 (* -0.021699 0.129)))
(+ 0.03 (* 0.2 (* 0.971659 0.0129)))


(+ hidden-output-strengths (* 0.2
                              (mapv #(* odeltas %) new-hidden-neurons)))

(defn update-strengths [deltas neurons strengths lrate]
  (+ strengths (* lrate
                  (mapv #(* deltas %) neurons))))

(testing "update strengths"
  (is (== [[0.14944734341306073 0.18320832546991603]
           [0.019086634528619688 0.06835597662949369]
           [0.009401783798869296 0.04512156124675721]]
          )
      (update-strengths
       odeltas
       new-hidden-neurons
       hidden-output-strengths
       learning-rate)))

(def new-hidden-output-strengths
  (update-strengths
       odeltas
       new-hidden-neurons
       hidden-output-strengths
       learning-rate))

(def new-input-hidden-strengths
  (update-strengths
       hdeltas
       input-neurons
       input-hidden-strengths
       learning-rate))

(testing "update strengths for hidden"
  (is (== [[0.14996511847614283 0.20551384334705303 0.13377615028650064]
           [0.01 0.02 0.03]]
          (update-strengths
           hdeltas
           input-neurons
           input-hidden-strengths
           learning-rate))))


;;; now we just need to put all the pieces together

;; create a matrix
;; prop forward input / feed forward
;; calc errors
;; update weights
(def nn [ [0 0] input-hidden-strengths hidden-neurons hidden-output-strengths [0 0]])

(defn feed-forward [input network]
  (let [[in i-h-strengths h h-o-strengths out] network
        new-h (layer-activation input i-h-strengths)
        new-o (layer-activation new-h h-o-strengths)]
    [input i-h-strengths new-h h-o-strengths new-o]))

(testing "feed forward"
  (is (== [input-neurons input-hidden-strengths new-hidden-neurons hidden-output-strengths new-output-neurons]
          (feed-forward [1 0] nn))))

(defn update-weights [network target learning-rate]
  (let [[ in i-h-strengths h h-o-strengths out] network
        o-deltas (output-deltas target out)
        h-deltas (hlayer-deltas o-deltas h h-o-strengths)
        n-h-o-strengths (update-strengths
                         o-deltas
                         h
                         h-o-strengths
                         learning-rate)
        n-i-h-strengths (update-strengths
                         h-deltas
                         in
                         i-h-strengths
                         learning-rate)]
    [in n-i-h-strengths h n-h-o-strengths out]))

(update-weights (feed-forward [1 0] nn) [0 1] 0.2)

(testing "update-weights"
  (is ( == [input-neurons
            new-input-hidden-strengths
            new-hidden-neurons
            new-hidden-output-strengths
            new-output-neurons]
           (update-weights (feed-forward [1 0] nn) [0 1] 0.2))))


(defn train-network [network input target learning-rate]
  (update-weights (feed-forward input network) target learning-rate))

(testing "train-network"
  (is (== [input-neurons
            new-input-hidden-strengths
            new-hidden-neurons
            new-hidden-output-strengths
           new-output-neurons]
          (train-network nn [1 0] [0 1] 0.2))))


;;;;;;;
;;;;;;
(def n1 (-> nn
     (train-network [1 0] [0 1] 0.5)
     (train-network [0.5 0] [0 0.5] 0.5)
     (train-network [0.25 0] [0 0.25] 0.5)
     ))

(defn ff [input network]
  (last (feed-forward input network)))

;;untrained
(ff [1 0] nn) ;=> [0.02315019005321053 0.027608061500083565]
;;trained
(ff [1 0] n1) ;=> [0.03765676393050254 0.10552175312900794]

;; lets get some recur on
(defn train-data [network data learning-rate]
  (if-let [[input target] (first data)]
    (recur
     (train-network network input target learning-rate)
     (rest data)
     learning-rate)
    network))

(def n2 (train-data nn [
                        [[1 0] [0 1]]
                        [[0.5 0] [0 0.5]]
                        [[0.25 0] [0 0.25] ]
                        ]
                    0.5))

(ff [1 0] n2) ;=> [0.03765676393050254 0.10552175312900794]

;; lazy training set of 1000 or something
(defn inverse-data []
  (let [n (rand 1)]
    [[n 0] [0 n]]))

(inverse-data)


(def n3 (train-data nn (repeatedly 400 inverse-data) 0.5))

(ff [1 0] n3) ;=> [-4.958278484025221E-4 0.8211647699205362]
(ff [0.5 0] n3) ;=> [2.1645760787874696E-4 0.5579396715416916]
(ff [0.25 0] n3) ;=> [1.8183385523103048E-4 0.31130601296149013]

(def n4 (train-data nn (repeatedly 1000 inverse-data) 0.2))

(ff [1 0] n4) ;=> [-4.954958580800465E-4 0.8160149309699489]
(ff [0.5 0] n4) ;=> [2.0984340203290242E-4 0.5577429793364974]
(ff [0.25 0] n4) ;=> [1.3457614704841535E-4 0.3061399408502212]


;;;;; how bout construct a generic neural network setup
(defn gen-strengths [to from]
  (let [l (* to from)]
    (map vec (partition from (repeatedly l #(rand (/ 1 l)))))))

(defn construct-network [num-in num-hidden num-out]
  (vec (map vec [(repeat num-in 0)
             (gen-strengths num-in num-hidden)
             (repeat num-hidden 0)
             (gen-strengths num-hidden num-out)
             (repeat num-out 0)])))



(def tnn (construct-network 2 3 2))
(def n5 (train-data tnn (repeatedly 1000 inverse-data) 0.2))
(ff [1 0] n4) ;=> [-4.954958580800465E-4 0.8160149309699489]














