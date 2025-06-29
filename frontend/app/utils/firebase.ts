// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import {
  browserSessionPersistence,
  getAuth,
  setPersistence,
} from "firebase/auth";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyBUgorceiKG-eMbF-xQ61nuzH5Fg26Z0QI",
  authDomain: "rmhi-9fea4.firebaseapp.com",
  projectId: "rmhi-9fea4",
  storageBucket: "rmhi-9fea4.firebasestorage.app",
  messagingSenderId: "427911737740",
  appId: "1:427911737740:web:b2525e0ffba6a1fc4126a9",
  measurementId: "G-YDL59408XB",
};
// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase Auth and get a reference to the service
export const auth = getAuth(app);
await setPersistence(auth, browserSessionPersistence);

// Initialize Analytics (optional)
export const analytics =
  typeof window !== "undefined" ? getAnalytics(app) : null;
