package com.drivetrack.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;
public class TripStore extends SQLiteOpenHelper {
 public TripStore(Context c){super(c,"trips.db",null,3);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE trips(id INTEGER PRIMARY KEY AUTOINCREMENT,started INTEGER,ended INTEGER,distance REAL,maxspeed REAL,movingms INTEGER,pausedms INTEGER DEFAULT 0,points TEXT,telemetry TEXT)");}
 public void onUpgrade(SQLiteDatabase db,int a,int b){if(a<2)db.execSQL("ALTER TABLE trips ADD COLUMN pausedms INTEGER DEFAULT 0");if(a<3)db.execSQL("ALTER TABLE trips ADD COLUMN telemetry TEXT");}
 public void save(long start,long end,double distance,double max,long moving,long paused,String points,String telemetry){ContentValues v=new ContentValues();v.put("started",start);v.put("ended",end);v.put("distance",distance);v.put("maxspeed",max);v.put("movingms",moving);v.put("pausedms",paused);v.put("points",points);v.put("telemetry",telemetry);getWritableDatabase().insert("trips",null,v);}
 public Cursor all(){return getReadableDatabase().rawQuery("SELECT * FROM trips ORDER BY started DESC",null);}
 public Cursor one(long id){return getReadableDatabase().rawQuery("SELECT * FROM trips WHERE id=?",new String[]{String.valueOf(id)});}
 public org.json.JSONObject exportAll() throws org.json.JSONException {
  org.json.JSONObject root=new org.json.JSONObject();
  root.put("format","DriveTrack JSON");root.put("version",2);
  root.put("units","metres,metres_per_second,unix_milliseconds");
  org.json.JSONArray journeys=new org.json.JSONArray();
  try(android.database.Cursor c=all()){
   while(c.moveToNext()){
    org.json.JSONObject item=new org.json.JSONObject();
    for(String key:new String[]{"id","started","ended","distance","maxspeed","movingms","pausedms"}){
     int col=c.getColumnIndex(key);
     if(col>=0)item.put(key,c.getString(col));
    }
    item.put("points",new org.json.JSONArray(c.getString(c.getColumnIndexOrThrow("points"))));
    int col=c.getColumnIndex("telemetry");
    item.put("telemetry",new org.json.JSONArray(col<0||c.isNull(col)?"[]":c.getString(col)));
    journeys.put(item);
   }
  }
  root.put("journeys",journeys);return root;
 }
 public void deleteAll(){getWritableDatabase().delete("trips",null,null);}
}
