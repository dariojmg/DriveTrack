package com.drivetrack.app;
import android.content.*;import android.database.*;import android.database.sqlite.*;
public class TripStore extends SQLiteOpenHelper {
 public TripStore(Context c){super(c,"trips.db",null,2);}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE trips(id INTEGER PRIMARY KEY AUTOINCREMENT,started INTEGER,ended INTEGER,distance REAL,maxspeed REAL,movingms INTEGER,pausedms INTEGER DEFAULT 0,points TEXT)");}
 public void onUpgrade(SQLiteDatabase db,int a,int b){if(a<2)db.execSQL("ALTER TABLE trips ADD COLUMN pausedms INTEGER DEFAULT 0");}
 public void save(long start,long end,double distance,double max,long moving,long paused,String points){ContentValues v=new ContentValues();v.put("started",start);v.put("ended",end);v.put("distance",distance);v.put("maxspeed",max);v.put("movingms",moving);v.put("pausedms",paused);v.put("points",points);getWritableDatabase().insert("trips",null,v);}
 public Cursor all(){return getReadableDatabase().rawQuery("SELECT * FROM trips ORDER BY started DESC",null);}
 public Cursor one(long id){return getReadableDatabase().rawQuery("SELECT * FROM trips WHERE id=?",new String[]{String.valueOf(id)});}
 public void deleteAll(){getWritableDatabase().delete("trips",null,null);}
}
