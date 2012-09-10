package com.android.maps.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class MapsDBReceiver extends BroadcastReceiver {

	double latitude,longitude;
	String msgbody,type,keyword,name,place,requesturl;
	int top;
	SmsMessage[] sms;
	SmsManager m;
	JSONObject json;
	ArrayList<String> msg1;
	List<Address> addr=null;
	HttpGet req;
	Object[] messages=null;
	HttpEntity jsonentity;
	HttpResponse res;
	HttpClient client;
	Geocoder g;
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		//Check if this is a SMS RECEIVED Broadcast.
		if(arg1.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
		{
			//Receive the SMS message in a string
			try {
				Bundle b=arg1.getExtras();
				messages = (Object[])b.get("pdus");
				sms=new SmsMessage[messages.length];
			} catch (Exception e3) {
				// TODO Auto-generated catch block
				Toast.makeText(arg0, e3.toString(), Toast.LENGTH_LONG).show();
			}
			for(int i=0;i<messages.length;i++)
			{
				sms[i]=SmsMessage.createFromPdu((byte [])messages[i]);
				msgbody=sms[i].getMessageBody();
				int hifcount=0;
				for(int x=0;x<msgbody.length();x++){
					if(msgbody.charAt(x)=='-'){
						hifcount++;
					}
				}
				if(hifcount!=5){
					m.sendTextMessage(sms[0].getOriginatingAddress(), null,"Format Wrong.", null, null);
				}
				Scanner scr=new Scanner(msgbody).useDelimiter("-");
				if(scr.next().equals("yuvislm"))
				{
					try {
						g=new Geocoder(arg0,Locale.getDefault());
						place=scr.next();
						type=scr.next();
						keyword=scr.next();
						name=scr.next();
						top=new Integer(scr.next()).intValue();
					}
					catch(NumberFormatException e){
						m.sendTextMessage(sms[0].getOriginatingAddress(), null,"Format Wrong:Extra Space Found.", null, null);
					}
					catch (Exception e1) {
						// TODO Auto-generated catch block
						Toast.makeText(arg0, e1.toString(), Toast.LENGTH_LONG).show();
					}
					try {
					addr= g.getFromLocationName(place, 5);
					} catch (Exception e2) {
						// TODO Auto-generated catch block
						Toast.makeText(arg0, e2.toString(), Toast.LENGTH_LONG).show();
					}
					if(!addr.isEmpty())
					{
						try {
						latitude=addr.get(0).getLatitude();
						longitude=addr.get(0).getLongitude();
						requesturl="https://maps.googleapis.com/maps/api/place/search/json?radius=500&sensor=false&key=<your KEY here>";
						requesturl+="&location="+new Double(latitude).toString()+","+new Double(longitude).toString();
						if(type!=null)
							requesturl+="&type="+type;
						if(keyword!=null)
							requesturl+="&keyword="+keyword;
						if(name!=null)
							requesturl+="&name="+name;
						Toast.makeText(arg0, new Double(latitude).toString()+" "+new Double(longitude).toString(), Toast.LENGTH_LONG).show();
						client=new DefaultHttpClient();
						req=new HttpGet(requesturl);
						res=client.execute(req);
						}
						catch(Exception e1){
							Toast.makeText(arg0, e1.toString(), Toast.LENGTH_LONG).show();
						}
						/*Toast.makeText(arg0, res.getStatusLine().toString(), Toast.LENGTH_LONG).show();*/
						if(res.getStatusLine().toString().equals("HTTP/1.1 200 OK"))
						{
							try {
								jsonentity=res.getEntity();
								InputStream in=jsonentity.getContent();
								//Toast.makeText(arg0, ConvertStreamToString(arg0,in), Toast.LENGTH_LONG).show();
								json=new JSONObject(convertStreamToString(arg0,in));
								JSONArray resarray=json.getJSONArray("results");
								Toast.makeText(arg0, new Integer(resarray.length()).toString(), Toast.LENGTH_LONG).show();
								StringBuilder msg=new StringBuilder();
								int len=0,j=0;
								m=SmsManager.getDefault();
								if(resarray.length()==0){
									msg.append("No results");
								}	
								else{
									if(resarray.length()>2){
										len=2*(top);
										j=len-2;
										if(len>resarray.length()){
											msg.append("Sorry top "+top+" result not available, Instead showing the last 2");
											len=resarray.length();
											j=len-2;
										}
									}
									else{
										j=0;
										len=resarray.length();
									}	
									for(;j<len;j++)
									{
										msg.append("\n"+(j+1)+":"+resarray.getJSONObject(j).getString("name"));
										msg.append("\nAddress:"+resarray.getJSONObject(j).getString("vicinity"));
										Toast.makeText(arg0, resarray.getJSONObject(j).getString("name") , Toast.LENGTH_LONG).show();
									}	
								}
								Toast.makeText(arg0,msg.toString().trim(), Toast.LENGTH_LONG).show();
								msg1=m.divideMessage(msg.toString().trim());
								m.sendMultipartTextMessage(sms[i].getOriginatingAddress(), null, msg1, null, null);
						}
						catch(Exception e) {
							// TODO Auto-generated catch block
							Toast.makeText(arg0,e.toString(), Toast.LENGTH_LONG).show();
							e.printStackTrace();
							}
						}
						else
						{
							Toast.makeText(arg0, res.getStatusLine().toString(), Toast.LENGTH_LONG).show();
						}
					}
					else
					{
						m.sendTextMessage(sms[0].getOriginatingAddress(), null,"Place not found.", null, null);
					}
				}
			}
		}
	}
	
	
	private String convertStreamToString(Context arg0,InputStream in) {
		// TODO Auto-generated method stub
		BufferedReader br=new BufferedReader(new InputStreamReader(in));
		StringBuilder jsonstr=new StringBuilder();
		String line;
		try {
			while((line=br.readLine())!=null)
			{
				String t=line+"\n";
				jsonstr.append(t);
			}
			br.close();
			if(jsonstr.equals(null)){
				Toast.makeText(arg0, "its a null", Toast.LENGTH_LONG).show();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Toast.makeText(arg0, "stream"+e.toString(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
		return jsonstr.toString();
	}
	
	
}

