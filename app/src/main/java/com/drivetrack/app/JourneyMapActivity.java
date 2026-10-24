package com.drivetrack.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Insets;
import android.database.Cursor;
import android.content.Context;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.*;
import org.json.JSONArray;

/** Dedicated map viewer: no nested scrolling, full pan/zoom, with recenter control. */
public class JourneyMapActivity extends Activity {
  private WebView web;
  private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  private Button button(String name,int bg){
    Button b=new Button(this);
    b.setText(name);b.setAllCaps(false);b.setTextColor(Color.WHITE);
    b.setTextSize(14);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bg));
    return b;
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    long id=getIntent().getLongExtra("journey_id",-1);
    JSONArray points;
    try(TripStore store=new TripStore(this);Cursor c=store.one(id)){
      if(!c.moveToFirst())throw new IllegalStateException("Journey not found");
      points=new JSONArray(c.getString(c.getColumnIndexOrThrow("points")));
    }catch(Exception e){
      new android.app.AlertDialog.Builder(this).setTitle("Could not load route")
        .setMessage(e.getMessage()).setPositiveButton("Close",(d,w)->finish()).show();
      return;
    }

    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(0xff0c131a);
    root.setOnApplyWindowInsetsListener((view,insets)->{
      int t,b;
      if(android.os.Build.VERSION.SDK_INT>=30){
        Insets bounds=insets.getInsets(WindowInsets.Type.systemBars());
        t=bounds.top;b=bounds.bottom;
      } else { t=insets.getSystemWindowInsetTop();b=insets.getSystemWindowInsetBottom(); }
      view.setPadding(0,t,0,b);
      return insets;
    });
    setContentView(root);root.requestApplyInsets();

    LinearLayout bar=new LinearLayout(this);bar.setPadding(dp(10),dp(3),dp(10),dp(3));
    bar.setGravity(Gravity.CENTER_VERTICAL);
    Button close=button("✕  Close",0xff223341);
    bar.addView(close,new LinearLayout.LayoutParams(dp(100),dp(52)));
    close.setOnClickListener(v->finish());
    TextView label=new TextView(this);label.setText("Journey Map");label.setTextSize(17);
    label.setTextColor(Color.WHITE);label.setGravity(Gravity.CENTER);
    bar.addView(label,new LinearLayout.LayoutParams(0,dp(52),1));
    Button recenter=button("⤢  Recenter",0xff17675b);
    bar.addView(recenter,new LinearLayout.LayoutParams(dp(125),dp(52)));
    recenter.setOnClickListener(v->{if(web!=null)web.evaluateJavascript("if(window.resetRoute){window.resetRoute();}",null);});
    root.addView(bar);

    if(points.length()<2){
      TextView empty=new TextView(this);
      empty.setText("This journey has no route to display.");empty.setTextColor(Color.WHITE);
      empty.setGravity(Gravity.CENTER);
      root.addView(empty,new LinearLayout.LayoutParams(-1,0,1));
      return;
    }
    web=new WebView(this);
    web.setBackgroundColor(0xff192733);
    WebSettings opts=web.getSettings();
    opts.setJavaScriptEnabled(true);
    opts.setDomStorageEnabled(false);
    opts.setAllowFileAccess(false);
    root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
    TextView hint=new TextView(this);hint.setText("Drag to explore • Pinch to zoom");
    hint.setTextSize(12);hint.setTextColor(0xffaec1ca);hint.setGravity(Gravity.CENTER);
    root.addView(hint,new LinearLayout.LayoutParams(-1,dp(34)));

    String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
      +"<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
      +"<style>html,body,#map{margin:0;width:100%;height:100%;background:#192733}</style></head>"
      +"<body><div id='map'></div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>"
      +"const coords="+points.toString()+";const map=L.map('map',{zoomControl:true,dragging:true,touchZoom:true,scrollWheelZoom:false});"
      +"L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap contributors'}).addTo(map);"
      +"const route=L.polyline(coords,{color:'#159cec',weight:5}).addTo(map);"
      +"L.circleMarker(coords[0],{color:'#00ddb3',fillColor:'#00ddb3',fillOpacity:1,radius:8}).addTo(map);"
      +"L.circleMarker(coords[coords.length-1],{color:'#ff536b',fillColor:'#ff536b',fillOpacity:1,radius:8}).addTo(map);"
      +"window.resetRoute=function(){map.fitBounds(route.getBounds(),{padding:[26,26]});};"
      +"window.resetRoute();setTimeout(()=>map.invalidateSize(),400);"
      +"</script></body></html>";
    web.loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null);
  }
  @Override public void onDestroy(){
    if(web!=null){web.destroy();web=null;}
    super.onDestroy();
  }
}
