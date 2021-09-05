package com.gkmhc.vedanta.nithya_panchangam;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.gkmhc.utils.VedicCalendar;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class NithyaPanchangamWidget extends AppWidgetProvider {
    SharedPreferences sharedPreferences;
    private static final String PREF_NP_LOCALE_KEY = "PREF_NP_LOCALE_KEY";
    public static final String PREF_LOCATION_DEF_VAL_KEY = "PREF_LOCATION_DEF_VAL_KEY";
    private double curLocationLongitude = 0; // Default to Varanasi
    private double curLocationLatitude = 0; // Default to Varanasi

    public NithyaPanchangamWidget() {
        // Required empty public constructor
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, String selLocale, Calendar currCalendar,
                                double longitude, double latitude,
                                HashMap<String, String[]> vedicCalendarLocaleList) {
        // For Widget, following fields are good enough to be displayed:
        // 1) Thithi
        // 2) Vaasaram
        // 3) Maasam
        // Form display string as, "Thithi, Vaasaram-Maasam"
        String location = MainActivity.readDefLocationSetting(context);
        int ayanamsaMode = MainActivity.readPrefAyanamsaSelection(context);
        VedicCalendar vedicCalendar = VedicCalendar.getInstance(
                VedicCalendar.PANCHANGAM_TYPE_DRIK_GANITHAM, currCalendar, longitude,
                latitude, MainActivity.getLocationTimeZone(location), ayanamsaMode,
                vedicCalendarLocaleList);
        if (vedicCalendar != null) {
            int refDinaangam =
                    vedicCalendar.getDinaAnkam(VedicCalendar.MATCH_SANKALPAM_EXACT);
            String vaasaramStr =
                    vedicCalendar.getVaasaram(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            String maasamStr =
                    vedicCalendar.getMaasam(VedicCalendar.MATCH_PANCHANGAM_PROMINENT);
            float textSize = 12f;

            // Increase font size for Sanskrit alone but keep default for Tamil & English
            if (selLocale.equalsIgnoreCase("Sa")) {
                textSize = 16f;
            }
            CharSequence widgetText = refDinaangam + ", " + vaasaramStr + "-" + maasamStr;
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.nithya_panchangam_widget);
            views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, textSize);
            views.setTextViewText(R.id.appwidget_text, widgetText);

            // Pop up the splash screen
            Intent openMainApp = new Intent(context, SplashScreen.class);
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, openMainApp, 0);
            views.setOnClickPendingIntent(R.id.widget_clock, pIntent);
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String prefLang = readLocaleSettings(context);
        String selLocale = MainActivity.getLocaleShortStr(prefLang);
        Locale locale = new Locale(selLocale);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Calendar currCalendar = Calendar.getInstance();
        long startTime = System.nanoTime();
        String localpath = context.getFilesDir() + File.separator + "/ephe";
        VedicCalendar.initSwissEph(localpath);
        long endTime = System.nanoTime();
        Log.d("NithyaPanchangamWidget","initSwissEph()... Time Taken: " +
                VedicCalendar.getTimeTaken(startTime, endTime));

        String curLocationCity = readDefLocationSetting();
        if (curLocationCity.isEmpty()) {
            curLocationCity = context.getString(R.string.pref_def_location_val);
        }
        getLocationCoords(curLocationCity, context);

        HashMap<String, String[]> vedicCalendarLocaleList =
                MainActivity.buildVedicCalendarLocaleList(context);

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, selLocale, currCalendar,
                            curLocationLongitude, curLocationLatitude, vedicCalendarLocaleList);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    /**
     * Utility function to get the default preferred location from the shared preferences.
     *
     * @return Selected locale as a string
     */
    private String readDefLocationSetting() {
        return sharedPreferences.getString(PREF_LOCATION_DEF_VAL_KEY, "");
    }

    public void getLocationCoords(String locationStr, Context context) {
        /*try {
            Geocoder geocoder = new Geocoder(context);
            List<Address> addressList = geocoder.getFromLocationName(locationStr, 1);
            if ((addressList != null) && (addressList.size() > 0)) {
                String strLocality = addressList.get(0).getLocality();
                curLocationLongitude = addressList.get(0).getLongitude();
                curLocationLatitude = addressList.get(0).getLatitude();
                Log.d("NithyaPanchangamWidget", "Location: " + strLocality +
                        " Longitude: " + curLocationLongitude +
                        " Latitude: " + curLocationLatitude);
            }
        } catch (Exception e) {
            // Nothing to do here.
            Log.d("MainActivity","Exception in initManualLocation()");
        }*/
        MainActivity.PlacesInfo placesInfo = MainActivity.getLocationFromPlacesDB(locationStr);
        curLocationLatitude = placesInfo.latitude;
        curLocationLongitude = placesInfo.longitude;
    }

    private String readLocaleSettings(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences != null) {
            return sharedPreferences.getString(PREF_NP_LOCALE_KEY, "En");
        }
        return "En";
    }
}