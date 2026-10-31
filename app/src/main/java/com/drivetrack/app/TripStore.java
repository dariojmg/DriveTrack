package com.drivetrack.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;
public class TripStore extends SQLiteOpenHelper {
 public TripStore(Context c){super(c,"trips.db",null,4);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE trips(id INTEGER PRIMARY KEY AUTOINCREMENT,started INTEGER,ended INTEGER,distance REAL,maxspeed REAL,movingms INTEGER,pausedms INTEGER DEFAULT 0,points TEXT,telemetry TEXT,title TEXT,notes TEXT)");}
 public void onUpgrade(SQLiteDatabase db,int a,int b){if(a<2)db.execSQL("ALTER TABLE trips ADD COLUMN pausedms INTEGER DEFAULT 0");if(a<3)db.execSQL("ALTER TABLE trips ADD COLUMN telemetry TEXT");if(a<4){db.execSQL("ALTER TABLE trips ADD COLUMN title TEXT");db.execSQL("ALTER TABLE trips ADD COLUMN notes TEXT");}}
 public void save(long start,long end,double distance,double max,long moving,long paused,String points,String telemetry){ContentValues v=new ContentValues();v.put("started",start);v.put("ended",end);v.put("distance",distance);v.put("maxspeed",max);v.put("movingms",moving);v.put("pausedms",paused);v.put("points",points);v.put("telemetry",telemetry);getWritableDatabase().insert("trips",null,v);}
 public Cursor all(){return getReadableDatabase().rawQuery("SELECT * FROM trips ORDER BY started DESC",null);}
 public Cursor one(long id){return getReadableDatabase().rawQuery("SELECT * FROM trips WHERE id=?",new String[]{String.valueOf(id)});}
 public org.json.JSONObject exportAll() throws org.json.JSONException {
  org.json.JSONObject root=new org.json.JSONObject();
  root.put("format","DriveTrack JSON");root.put("version",2);
  root.put("app_version",com.drivetrack.app.BuildConfig.VERSION_NAME);
  root.put("exported_at",System.currentTimeMillis());
  root.put("units","metres,metres_per_second,unix_milliseconds");
  org.json.JSONArray journeys=new org.json.JSONArray();
  try(android.database.Cursor c=all()){
   while(c.moveToNext()){
    org.json.JSONObject item=new org.json.JSONObject();
    for(String key:new String[]{"id","started","ended","distance","maxspeed","movingms","pausedms","title","notes"}){
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
    int telemetryColumn=-1,titleColumn=-1,notesColumn=-1;
    try(Cursor columns=source.rawQuery("PRAGMA table_info(trips)",null)){
     while(columns.moveToNext()){
      String col=columns.getString(1);
      if("telemetry".equals(col))telemetryColumn=1;
      if("title".equals(col))titleColumn=1;
      if("notes".equals(col))notesColumn=1;
     }
    }
    String select="SELECT started,ended,distance,maxspeed,movingms,pausedms,points"+
      (telemetryColumn>=0?",telemetry":" ,NULL AS telemetry")+
      (titleColumn>=0?",title":" ,NULL AS title")+
      (notesColumn>=0?",notes":" ,NULL AS notes")+" FROM trips ORDER BY started";
    try(Cursor rows=source.rawQuery(select,null)){
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
       v.put("points",pts);
       String telemetry=rows.getString(7);
       new org.json.JSONArray(telemetry==null?"[]":telemetry);
       v.put("telemetry",telemetry==null?"[]":telemetry);
       v.put("title",rows.getString(8));v.put("notes",rows.getString(9));
       if(destination.insertOrThrow("trips",null,v)>0)imported++;
      }
      destination.setTransactionSuccessful();
     }finally{destination.endTransaction();}
    }
   }
   return imported;
  }finally{tmp.delete();}
 }
 public void rename(long id,String title,String notes){
  ContentValues c=new ContentValues();c.put("title",title);c.put("notes",notes);
  getWritableDatabase().update("trips",c,"id=?",new String[]{String.valueOf(id)});
 }
 public int countEmpty(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM trips WHERE distance<20",null)){return c.moveToFirst()?c.getInt(0):0;}}
 public int deleteEmpty(){return getWritableDatabase().delete("trips","distance<20",null);}
 public void deleteOne(long id){getWritableDatabase().delete("trips","id=?",new String[]{String.valueOf(id)});}
 public void deleteAll(){getWritableDatabase().delete("trips",null,null);}
}
