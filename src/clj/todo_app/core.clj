(ns todo-app.core
  (:require [ring.adapter.jetty :as jetty]
            [compojure.handler :as handler]
            [compojure.route :refer [not-found resources]]
            [ring.util.response :as resp]
            [todo-app.database :as db])
  (:use [compojure.core]))

(use 'ring.middleware.params
     'ring.middleware.session)

(def session-data (atom {}))

(defn set-user-session [request email]
    (let [session-id (str (java.util.UUID/randomUUID))]
        (do (swap! session-data assoc-in [session-id] email)
            (assoc (resp/redirect "/") :session session-id))))

(defn delete-task [params email]
    (let [task-id (get params "task-id")]
        (db/delete-task email task-id)
        (resp/response "")))

(defn update-task [params email]
    (let [task-id (get params "task-id")
          task-name (get params "task-name")
          due-date (get params "due-date")
          task-body {"task-name" task-name
                     "due-date" due-date
                     "status" "Completed"}]
    (db/update-task email task-id task-body)
    (resp/response "")))

(defn create-task [params email]
    (let [task-name (get params "task-name")
          due-date (get params "due-date")
          task-id (str (java.util.UUID/randomUUID))
          task-body {"task-name" task-name
                     "due-date" due-date
                     "status" "In Progress"}]
     (db/create-task email task-id task-body)
     (resp/response task-id)))

(defn logout [request]
    (assoc (resp/redirect "/") :session nil))

(defn handler-home [request]
    (let [params (:params request)
          action (get params "action")
          session-id (:session request)
          email (get @session-data session-id)]
      (case action
        "create" (create-task params email)
        "update" (update-task params email)
        "delete" (delete-task params email))))

(defn handler-login [request]
    (let [params (:params request)
          email (get params "email")
          password (get params "password")]
        (if (db/valid-user? email password)
            (set-user-session request email)
            (resp/redirect "/login"))))

(defn handler-signup [request]
    (let [params (:params request)
          full-name (get params "full-name")
          email (get params "email")
          password (get params "password")]
        (if (db/user-exists? email)
            (resp/redirect "/signup")
            (do (db/create-user email full-name password)
                (resp/redirect "login")))))

(defn login [request]
    (slurp "./resources/public/login.html"))

(defn signup [request]
    (slurp "./resources/public/signup.html"))

(defn main [request]
    (prn request)
    (if (empty? (:session request))
        (slurp "./resources/public/index.html")
        (slurp "./resources/public/main.html")))

(defroutes main-routes
  (GET "/" [] main)
  (POST "/" [] handler-home)
  (GET "/login" [] login)
  (POST "/login" [] handler-login)
  (GET "/signup" [] signup)
  (POST "/signup" [] handler-signup)
  (GET "/logout" [] logout)
  (resources "/")
  (not-found (slurp "./resources/public/error_404.html")))

(def app (wrap-session (wrap-params main-routes)))

(defn -main []
  (jetty/run-jetty app {:port 3030}))