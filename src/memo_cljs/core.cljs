(ns memo-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer (<! timeout)]))

(enable-console-print!)

(defn generate-cards []
  (let [cards (mapcat (partial repeat 2) (range 16))]
    (shuffle cards)))

(def app-state (atom {:deck (mapv #(hash-map :kind % :face-up false :on-deck true) (generate-cards))
                      :players [{:name "Igor" :score 0}
                                {:name "Lina" :score 0}]
                      :current-player 0
                      :deck-disabled false}))

(defn face-up-cards [{deck :deck}]
  (filter #(= (:face-up %) true) deck))

(defn current-player [app-state]
  (nth (:players app-state) (:current-player app-state)))

(defn switch-to-next-player [app-state]
  (let [new-deck (mapv #(assoc % :face-up false) (:deck app-state))
        next-player (inc (:current-player app-state))
        next-player (rem next-player (count (:players app-state)))
        new-state (assoc app-state :deck new-deck :current-player next-player)]
    new-state))

(defn inc-score [app-state]
  (let [new-state (update-in app-state [:players (:current-player app-state) :score] + 2)]
    (update-in new-state [:deck]
               (fn [deck] (mapv #(if (:face-up %) (assoc % :on-deck false) %) deck)))))

(defn wait-and-go-on [app-state win?]
  (swap! app-state #(assoc % :deck-disabled true))
  (go
   (<! (timeout (if win? 500 1500)))
   (swap! app-state #(assoc % :deck-disabled false))
   (when win? (swap! app-state inc-score))
   (swap! app-state switch-to-next-player)))

(defn make-move [_ card]
  (when-not (or (:deck-disabled @app-state) (:face-up @card))
    (om/transact! card #(assoc % :face-up true))
    (let [face-up (face-up-cards @app-state)
          next-player-move? (= (count face-up) 2)
          cards-equal? (if next-player-move?
                         (= (:kind (nth face-up 0)) (:kind (nth face-up 1)))
                         false)]
      (when next-player-move?
        (wait-and-go-on app-state cards-equal?)))))

(defn card-view [card owner]
  (reify
    om/IRender
    (render [_]
      (let [class (str "card" (when (:face-up card) (str " faceup kind-" (:kind card))))
            class (str class (when-not (:on-deck card) " empty"))
            props #js {:className class
                       :onClick (when (:on-deck card) #(make-move % card))}]
      (dom/div props)))))

(defn deck-view [deck owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "deck"}
        (let [deck (partition 8 deck)]
          (map #(apply dom/div
                        #js {:className "row"}
                        (om/build-all card-view (nth deck %)))
                (range (count deck))))))))

(defn game-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil (str "Current player: " (:name (current-player @app))))
        (apply dom/div nil (map #(dom/h2 nil (:name %) ": " (:score %)) (:players app)))
        (om/build deck-view (:deck app))))))

(om/root app-state game-view (. js/document (getElementById "game")))
