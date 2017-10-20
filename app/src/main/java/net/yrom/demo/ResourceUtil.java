package net.yrom.demo;

import android.content.Context;

public class ResourceUtil {
	 public static int getLayoutId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "layout", paramContext.getPackageName());
	 }

	 public static int getStringId(Context paramContext, String paramString) {
		 int stringId = paramContext.getResources().getIdentifier(paramString, "string", paramContext.getPackageName());
		 if(stringId == 0){
			 stringId = paramContext.getResources().getIdentifier("net_error_0", "string", paramContext.getPackageName());
		 }
	    return stringId;
	 }
	 
	 public static String getString(Context paramContext,String paramString){
		 int stringId = paramContext.getResources().getIdentifier(paramString, "string", paramContext.getPackageName());
		 if(stringId == 0){
			 return "";
		 }
		 return paramContext.getString(stringId);
	 }

	 public static int getDrawableId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "drawable", paramContext.getPackageName());
	 }

	 public static int getStyleId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "style", paramContext.getPackageName());
	 }

	 public static int getId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "id", paramContext.getPackageName());
	 }

	 public static int getColorId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "color", paramContext.getPackageName()); 
	 }

	 public static int getDimenId(Context paramContext, String paramString) {
	    return paramContext.getResources().getIdentifier(paramString, "dimen", paramContext.getPackageName());
	 }
	 
	 public static int getAnimId(Context paramContext, String paramString) {
		    return paramContext.getResources().getIdentifier(paramString, "anim", paramContext.getPackageName());
		 }
	 
	 public static int getArrayId(Context paramContext, String paramString) {
		return paramContext.getResources().getIdentifier(paramString, "array", paramContext.getPackageName());
	}	
	 
	public static int getstyleableId(Context paramContext, String paramString) {
		    return paramContext.getResources().getIdentifier(paramString, "styleable", paramContext.getPackageName());
	}
	 
	public static int [] getstyleableArray(Context paramContext, String paramString) {
		int i = paramContext.getResources().getIdentifier(paramString, "styleable", paramContext.getPackageName());
		return paramContext.getResources().getIntArray(i);
    }
}
