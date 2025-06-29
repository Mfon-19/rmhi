import { cert, getApps, initializeApp, App } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import serviceAccount from '../secret/firebase/serviceAccount.json';

let app: App;

if (!getApps().length) {
  app = initializeApp({
    credential: cert(serviceAccount as unknown as object), 
  });
} else {
  app = getApps()[0]; 
}

export const adminAuth = getAuth(app); 
export default app; 
