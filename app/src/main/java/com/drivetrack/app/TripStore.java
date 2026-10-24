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
 public int importDatabase(Context context,android.net.Uri uri) throws Exception {
  java.io.File tmp=java.io.File.createTempFile("drivetrack-import-", ".db",context.getCacheDir());
  try {
   try(java.io.InputStream in=context.getContentResolver().openInputStream(uri);
       java.io.FileOutputStream out=new java.io.FileOutputStream(tmp)){
    if(in==null)throw new java.io.IOException("Backup not accessible");
    byte[] buffer=new byte[16384];int count;long total=0;
    while((count=in.read(buffer))!=-1){total+=count;if(total>100*1024*1024)throw new java.io.IOException("Backup exceeds 100 MB");out.write(buffer,0,count);}
   }
   int imported=0;
   try(SQLiteDatabase source=SQLiteDatabase.openDatabase(tmp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
       Cursor verify=source.rawQuery("PRAGMA integrity_check",null)){
    if(!verify.moveToFirst()||!"ok".equalsIgnoreCase(verify.getString(0)))throw new java.io.IOException("SQLite backup failed integrity check");
    try(Cursor rows=source.rawQuery("SELECT started,ended,distance,maxspeed,movingms,pausedms,points FROM trips ORDER BY started",null)){
     SQLiteDatabase destination=getWritableDatabase();
     destination.beginTransaction();
     try {
      while(rows.moveToNext()){
       long start=rows.getLong(0),end=rows.getLong(1);
       if(start<=0||end<start)throw new java.io.IOException("Invalid timestamps in backup");
       String pts=rows.getString(6);
       new org.json.JSONArray(pts==null?"[]":pts);
       try(Cursor existing=destination.rawQuery("SELECT 1 FROM trips WHERE started=? AND ended=? LIMIT 1",new String[]{String.valueOf(start),String.valueOf(end)})){
        if(existing.moveToFirst())continue;
       }
       ContentValues v=new ContentValues();
       v.put("started",start);v.put("ended",end);
       v.put("distance",rows.getDouble(2));v.put("maxspeed",rows.getDouble(3));
       v.put("movingms",rows.getLong(4));v.put("pausedms",rows.getLong(5));
       v.put("points",pts);v.put("telemetry","[]");
       if(destination.insertOrThrow("trips",null,v)>0)imported++;
      }
      destination.setTransactionSuccessful();
     }finally{destination.endTransaction();}
    }
   }
   return imported;
  }finally{tmp.delete();}
 }
 public void deleteAll(){getWritableDatabase().delete("trips",null,null);}
}
