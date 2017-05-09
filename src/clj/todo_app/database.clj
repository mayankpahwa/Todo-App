(ns todo-app.database
	(:require  [clojure.string :as str]
           	   [taoensso.carmine :as car :refer (wcar)]
           	   [crypto.password.pbkdf2 :as pass]))

(def server-conn {:pool {}
				  :spec {:host "localhost"
                         :port  6379
                         :timeout-ms  4000}})

(defmacro wcar* [& body] `(car/wcar server-conn ~@body))


(defn user-exists? [email]
	(case (wcar* (car/exists email))
	  	  0 nil
	  	  1 "true"))

(defn valid-user? [email password]
	(let [db-password (first (wcar* (car/hmget email "password")))]
		(if (pass/check password db-password)
			"True")))
	
(defn create-user [email full-name password]
	(wcar* (car/hmset email "full-name" full-name "password" (pass/encrypt password))))

(defn create-task [email task-id task-body]
	(wcar* (car/hmset email task-id task-body)))

(defn update-task [email task-id task-body]
	(wcar* (car/hmset email task-id task-body)))

(defn delete-task [email task-id]
	(wcar* (car/hdel email task-id)))