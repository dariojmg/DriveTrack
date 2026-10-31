package com.drivetrack.app;
import android.app.*;import android.content.*;import android.content.pm.ServiceInfo;import android.location.*;import android.os.*;import org.json.*;
public class TrackingService extends Service implements LocationListener {
 public static final String UPDATE="com.drivetrack.app.UPDATE";
 public static boolean active=false,paused=false;
 public static long pausedMs=0,pausedAt=0;
 public static long elapsedActiveMs(){return active?Math.max(0,(paused?pausedAt:System.currentTimeMillis())-started-pausedMs):0;}
 public static long started=0,movingMs=0;
 public static double distance=0,maxSpeed=0,currentSpeed=0;
 public static double lastRawSpeed=0;
 public static float speedAccuracy=Float.NaN;
 public static String filterMode="waiting";
 public static String gpsConfidenceNote="";
 private int questionableLowSpeedSamples=0;
 public static long lastAcceptedFixAgeMs(){return lastFixTime<=0?-1:System.currentTimeMillis()-lastFixTime;}
 public static double visibleSpeed(){return active&&!paused&&lastFixTime>0&&lastAcceptedFixAgeMs()<3500?currentSpeed:0;}
 public static float accuracy=0;
 public static long lastFixTime=0;
 public static int rejectedFixes=0;
 public static String locationText="Waiting for GPS";
 public static String gpsIssue="";
 public static JSONArray points=new JSONArray(),telemetry=new JSONArray();
 private LocationManager lm;private Location last;private long lastAt=0;private double previousValidSpeed=-1;
 public void onCreate(){super.onCreate();lm=(LocationManager)getSystemService(LOCATION_SERVICE);NotificationChannel ch=new NotificationChannel("journey","Journey tracking",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){finish();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;}
  String action=i==null?null:i.getAction();
  if("PAUSE".equals(action)&&active&&!paused){
   paused=true;pausedAt=System.currentTimeMillis();currentSpeed=0;filterMode="paused";last=null;lastAt=0;previousValidSpeed=-1;
   try{lm.removeUpdates(this);}catch(Exception ignored){}
   updateNotification();sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));return START_STICKY;
  }
  if("RESUME".equals(action)&&active&&paused){
   pausedMs+=System.currentTimeMillis()-pausedAt;paused=false;pausedAt=0;currentSpeed=0;previousValidSpeed=-1;filterMode="acquiring";last=null;lastAt=0;
   updateNotification();
   try{lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);}catch(SecurityException ignored){}
   sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));return START_STICKY;
  }
  if(!active){started=System.currentTimeMillis();movingMs=0;pausedMs=0;pausedAt=0;paused=false;distance=0;maxSpeed=0;currentSpeed=0;rejectedFixes=0;lastFixTime=0;points=new JSONArray();telemetry=new JSONArray();last=null;lastAt=0;previousValidSpeed=-1;lastRawSpeed=0;speedAccuracy=Float.NaN;filterMode="acquiring";gpsConfidenceNote="";questionableLowSpeedSamples=0;active=true;getSharedPreferences("recovery",0).edit().putLong("started",started).apply();}
  updateNotification();
  gpsIssue="";
  try{
   if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))gpsIssue="Turn on Location services";
   else lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
  }catch(SecurityException err){gpsIssue="Location permission required";}
  catch(Exception err){gpsIssue="GPS provider unavailable";}
  sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
  return START_STICKY;
 }
 private void updateNotification(){
  Notification n=new Notification.Builder(this,"journey").setContentTitle(paused?"DriveTrack paused":"DriveTrack is recording").setContentText(paused?"Journey recording is paused":"Tap to view your journey").setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,ModernActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();
  if(Build.VERSION.SDK_INT>=29)startForeground(9,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);else startForeground(9,n);
 }
 public void onLocationChanged(Location p){
  if(!active||paused)return;
  gpsIssue="";
  accuracy=p.hasAccuracy()?p.getAccuracy():999;
  if(accuracy>35){rejectedFixes++;return;}
  long now=p.getElapsedRealtimeNanos()/1000000L;
  double rawSpeed=p.hasSpeed()?Math.max(0,p.getSpeed()):0;
  speedAccuracy=Build.VERSION.SDK_INT>=26&&p.hasSpeedAccuracy()?p.getSpeedAccuracyMetersPerSecond():Float.NaN;
  if(p.hasSpeed()&&!Float.isNaN(speedAccuracy)&&speedAccuracy>5.0f){rejectedFixes++;filterMode="uncertain speed";return;}
  double speed=rawSpeed;
  if(rawSpeed>65){rejectedFixes++;return;}
  if(previousValidSpeed>=0&&lastAt>0){
   double seconds=Math.max(.2,(now-lastAt)/1000.0);
   if(Math.abs(rawSpeed-previousValidSpeed)>Math.max(7,6*seconds)){rejectedFixes++;return;}
  }
  if(!p.hasSpeed()&&last!=null&&lastAt>0){
   double seconds=(now-lastAt)/1000.0;
   if(seconds>.2&&seconds<15)speed=Math.min(65,last.distanceTo(p)/seconds);
  }
  if(last!=null){long dt=now-lastAt;float d=last.distanceTo(p);double inferred=dt>0?d/(dt/1000.0):999;
   if(dt>0&&dt<30000&&inferred<65&&d>Math.max(3,accuracy*.35)){
    distance+=d;if(speed>0.8)movingMs+=dt;
   }
  }
  // Adaptive smoothing: quick response to real acceleration/braking, calm at steady speed.
  // Time-aware alpha keeps behaviour consistent when GPS callbacks are irregular.
  if(previousValidSpeed<0){currentSpeed=speed;filterMode="new fix";}
  else {
   double dtSeconds=Math.max(.2,Math.min(3.0,(now-lastAt)/1000.0));
   double change=Math.abs(speed-currentSpeed);
   double baseAlpha=change>=2.0?.82:change>=.7?.68:.45;
   // Avoid reporting motion from minor GPS drift while stationary.
   if(speed<.6&&currentSpeed<1.5)baseAlpha=.85;
   if(!Float.isNaN(speedAccuracy)&&speedAccuracy>2.5f)baseAlpha=Math.min(baseAlpha,.55);
   double alpha=1-Math.pow(1-baseAlpha,dtSeconds);
   filterMode=baseAlpha>=.68?"responsive":"steady";
   currentSpeed=Math.max(0,currentSpeed+alpha*(speed-currentSpeed));
   if(speed<.35&&currentSpeed<.6)currentSpeed=0;
  }
  // A low GPS speed with deteriorated positional accuracy can be ambiguous,
  // especially around roundabouts. Never invent speed or distance: surface uncertainty.
  if(speed>0.4&&speed<2.3&&accuracy>=7.0f)questionableLowSpeedSamples++;
  else questionableLowSpeedSamples=0;
  gpsConfidenceNote=questionableLowSpeedSamples>=5?"GPS speed uncertain · check vehicle speed":"";
  if(questionableLowSpeedSamples>=5)filterMode="low-speed-uncertain";
  lastRawSpeed=rawSpeed;previousValidSpeed=speed;maxSpeed=Math.max(maxSpeed,speed);
  try{org.json.JSONArray sample=new org.json.JSONArray();
   sample.put(System.currentTimeMillis());sample.put(Math.round(rawSpeed*100.0)/100.0);
   sample.put(Math.round(currentSpeed*100.0)/100.0);sample.put(accuracy);
   sample.put(Float.isNaN(speedAccuracy)?JSONObject.NULL:Math.round(speedAccuracy*100.0)/100.0);
   sample.put(filterMode);
   telemetry.put(sample);
  }catch(Exception ignored){}
  last=p;lastAt=now;lastFixTime=System.currentTimeMillis();
  locationText=String.format(java.util.Locale.US,"%.5f, %.5f",p.getLatitude(),p.getLongitude());
  try { JSONArray pair=new JSONArray();pair.put(p.getLatitude());pair.put(p.getLongitude());pair.put(System.currentTimeMillis());points.put(pair); } catch (JSONException ignored) {}
  getSharedPreferences("recovery",0).edit().putLong("started",started).putFloat("distance",(float)distance).putFloat("max",(float)maxSpeed).putLong("moving",movingMs).putString("points",points.toString()).apply();
  sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
 }
 private void finish(){
  if(!active)return;
  if(paused){pausedMs+=System.currentTimeMillis()-pausedAt;paused=false;pausedAt=0;}
  active=false;currentSpeed=0;
  try{lm.removeUpdates(this);}catch(Exception ignored){}
  // Ignore accidental taps and journeys without meaningful GPS distance.
  if(distance>=20&&points.length()>=3)
   new TripStore(this).save(started,System.currentTimeMillis(),distance,maxSpeed,movingMs,pausedMs,points.toString(),telemetry.toString());
  getSharedPreferences("recovery",0).edit().clear().apply();
  distance=0;maxSpeed=0;currentSpeed=0;movingMs=0;accuracy=0;locationText="Waiting for GPS";lastFixTime=0;lastRawSpeed=0;speedAccuracy=Float.NaN;filterMode="waiting";gpsConfidenceNote="";questionableLowSpeedSamples=0;points=new JSONArray();telemetry=new JSONArray();last=null;lastAt=0;
  sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));
 }
 public void onDestroy(){if(active)try{lm.removeUpdates(this);}catch(Exception ignored){}super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
