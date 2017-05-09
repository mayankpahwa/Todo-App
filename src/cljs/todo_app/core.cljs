(ns todo-app.core
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]
            [cljs-uuid-utils.core :as uuid]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(def app-state (atom {}))

(defn get-color [task-id]
  (let [task-status (get-in @app-state [task-id "status"])]
    (case task-status
      "In Progress" "#bf3a2d"
      "Completed" "green")))

(defn delete-task [task-id]
  (go 
    (let [response (<! (http/post "/" {:form-params {:action "delete" :task-id task-id}}))]
      (if (= 200 (:status response))
          (reset! app-state (dissoc @app-state task-id))))))

(defn delete-task-handler [task-id]
  (case (js/confirm "Are you sure you want to delete this task?")
    true (delete-task task-id)
    false nil))


(defn complete-task [task-id task-name due-date]
  (go 
    (let [response (<! (http/post "/" {:form-params {:action "update" :task-id task-id :task-name task-name :due-date due-date}}))
          xyz (prn response)]
      (if (= 200 (:status response))
          (swap! app-state assoc-in [task-id "status"] "Completed")))))


(defn create-task [task-name due-date]
  (go 
    (let [response (<! (http/post "/" {:form-params {:action "create" :task-name task-name, :due-date due-date}}))
          task-id (:body response)
          task-details {"task-name" task-name
                        "due-date" due-date
                        "status" "In Progress"}]
      (do (swap! app-state assoc-in [task-id] task-details)
          (set! (.-value (.getElementById js/document "task_name")) "")
          (set! (.-value (.getElementById js/document "date")) "")))))

(defn create-task-handler []
  (let [task-name (str/trim (.-value (.getElementById js/document "task_name")))
        due-date (.-value (.getElementById js/document "date"))]
    (cond
      (empty? task-name) (js/alert "Please enter a task")
      (empty? due-date) (js/alert "Please enter a date")
      (> (count task-name) 40) (js/alert "Task name cannot be more than 40 characters")
      :else (create-task task-name due-date))))


(defn individual-task-body [task-id task-name due-date]
  [:table {:class "individual-task"}
       [:tbody 
          [:tr 
            [:td {:class "material-icons"
                  :on-click (fn [e] (complete-task task-id task-name due-date))}
                  "done"]
            [:td {:class "individual-task-details" :id task-id :style {:color (get-color task-id)}} task-name
                 [:span {:id "due-date"} due-date]]
            [:td {:class "material-icons"
                  :on-click (fn [e] (delete-task-handler task-id))} "delete"]]]])

(defn todo []
 [:div {:id "all-tasks"}
  (for [task-id (keys @app-state)]
    (let [task-name (get-in @app-state [task-id "task-name"])
          due-date  (get-in @app-state [task-id "due-date"])]
      (individual-task-body task-id task-name due-date)))])


(.addEventListener (.getElementById js/document "create-task-button") "click" create-task-handler)

(reagent/render-component [todo]
                          (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)