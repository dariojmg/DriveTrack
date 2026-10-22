package com.drivetrack.app;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import org.json.*;
public class TrackingService extends Service implements LocationListener {
 public static final String UPDATE="com.drivetrack.app.UPDATE";
 public static boolean active=false,paused=false;
 public static long pausedMs=0,pausedAt=0;
 public static long elapsedActiveMs(){return active?Math.max(0,(paused?pausedAt:System.currentTimeMillis())-started-pausedMs):0;}
 public static long started=0,movingMs=0;
 public static double distance=0,maxSpeed=0,currentSpeed=0;
 public static float accuracy=0;
 public static String locationText="Waiting for GPS";
 public static JSONArray points=new JSONArray();
 private LocationManager lm;private Location last;private long lastAt=0;
 public void onCreate(){super.onCreate();lm=(LocationManager)getSystemService(LOCATION_SERVICE);NotificationChannel ch=new NotificationChannel("journey","Journey tracking",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){finish();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
  String action=i==null?null:i.getAction();
  if("PAUSE".equals(action)&&active&&!paused){
   paused=true;pausedAt=System.currentTimeMillis();currentSpeed=0;last=null;lastAt=0;
   try{lm.removeUpdates(this);}catch(Exception ignored){}
   updateNotification();sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));return START_STICKY;
  }
  if("RESUME".equals(action)&&active&&paused){
   pausedMs+=System.currentTimeMillis()-pausedAt;paused=false;pausedAt=0;currentSpeed=0;last=null;lastAt=0;
   updateNotification();
   try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);}catch(SecurityException ignored){}
   sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));return START_STICKY;
  }
  if(!active){started=System.currentTimeMillis();movingMs=0;pausedMs=0;pausedAt=0;paused=false;distance=0;maxSpeed=0;currentSpeed=0;points=new JSONArray();last=null;lastAt=0;active=true;getSharedPreferences("recovery",0).edit().putLong("started",started).apply();}
  updateNotification();
  try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);}catch(SecurityException ignored){}
  return START_STICKY;
 }
 private void updateNotification(){
  Notification n=new Notification.Builder(this,"journey").setContentTitle(paused?"DriveTrack paused":"DriveTrack is recording").setContentText(paused?"Journey recording is paused":"Tap to view your journey").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();
  if(Build.VERSION.SDK_INT>=29)startForeground(9,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(9,n);
 }
 public void onLocationChanged(Location p){
  if(!active||paused)return;
  accuracy=p.hasAccuracy()?p.getAccuracy():999;
  if(accuracy>35)return;
  long now=p.getElapsedRealtimeNanos()/1000000L;
  double speed=p.hasSpeed()?Math.max(0,p.getSpeed()):0;
  if(last!=null){long dt=now-lastAt;float d=last.distanceTo(p);double inferred=dt>0?d/(dt/1000.0):999;
   if(dt>0&&dt<30000&&inferred<65&&d>Math.max(3,accuracy*.35)){
    distance+=d;if(speed>0.8)movingMs+=dt;
   }
  }
  if(speed<65){currentSpeed=speed;maxSpeed=Math.max(maxSpeed,speed);}
  last=p;lastAt=now;
  locationText=String.format(java.util.Locale.US,"%.5f, %.5f",p.getLatitude(),p.getLongitude());
  try { JSONArray pair=new JSONArray();pair.put(p.getLatitude());pair.put(p.getLongitude());points.put(pair); } catch (JSONException ignored) {}
  getSharedPreferences("recovery",0).edit().putLong("started",started).putFloat("distance",(float)distance).putFloat("max",(float)maxSpeed).putLong("moving",movingMs).putString("points",points.toString()).apply();
  sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
 }
 private void finish(){
  if(!active)return;
  if(paused){pausedMs+=System.currentTimeMillis()-pausedAt;paused=false;pausedAt=0;}
  active=false;currentSpeed=0;
  try{lm.removeUpdates(this);}catch(Exception ignored){}
  new TripStore(this).save(started,System.currentTimeMillis(),distance,maxSpeed,movingMs,pausedMs,points.toString());
  getSharedPreferences("recovery",0).edit().clear().apply();
  sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
 }
 public void onDestroy(){if(active)try{lm.removeUpdates(this);}catch(Exception ignored){}super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
