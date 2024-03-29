package max.com.realtimetransportodessa;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import max.com.realtimetransportodessa.model.Master;
import max.com.realtimetransportodessa.model.Point;
import max.com.realtimetransportodessa.model.Route;
import max.com.realtimetransportodessa.model.Segment;
import max.com.realtimetransportodessa.model.State;
import max.com.realtimetransportodessa.model.Transport;

public class Loader {
    private static final String TAG = "Loader";
    private static ContentProvider contentProvider;
    private static Loader loader;
    private Context context;
    private RequestQueue requestQueue;
    private Map<String, String> urls;

    private Loader(Context context, ContentProvider contentProvider) {
        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
        Loader.contentProvider = contentProvider;

        urls = new HashMap<>();
        urls.put("LoadingListRoutes", "http://transport.odessa.ua/php/LoadingListRoutes.php");
        urls.put("LoadingRoute", "http://transport.odessa.ua/php/LoadingRoute.php");
        urls.put("LoadingListStopping", "http://transport.odessa.ua/php/LoadingListStopping.php");
        urls.put("LoadingListMaster", "http://transport.odessa.ua/php/LoadingListMaster.php");
        urls.put("GetState", "http://transport.odessa.ua/php/getState.php");
    }

    public static Loader getInstance(Context context, ContentProvider contentProvider) {
        if (loader == null) {
            loader = new Loader(context, contentProvider);
        }
        return loader;
    }

    public static Loader getInstance() {
        return loader;
    }

    public void loadRoutesList() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                urls.get("LoadingListRoutes"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if(json.getBoolean("success")) {
                                List<Route> routes = new ArrayList<>();
                                JSONArray routesJson = json.getJSONArray("list");
                                for(int i = 0; i < routesJson.length(); i++) {
                                    JSONObject routeJSON = routesJson.getJSONObject(i);
                                    Route route = Route.newBuilder()
                                            .setId(routeJSON.getInt("id"))
                                            .setNumber(routeJSON.getInt("Number"))
                                            .setType(routeJSON.getString("Type"))
                                            .setColor(routeJSON.getString("color"))
                                            .setCost(routeJSON.getDouble("cost"))
                                            .setDistance(routeJSON.getDouble("distance"))
                                            .setTitle(routeJSON.getString("title")).build();
                                    routes.add(route);
                                }
                                contentProvider.setRouteList(routes);
                                //Log.d(TAG, "Got routes list: " + routes.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Load routes list error: ", error);
                    }
                });
        requestQueue.add(stringRequest);
    }

    public void loadRoute(final String type, final String number, final String language) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                urls.get("LoadingRoute"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if(json.getBoolean("success")) {
                                Route.Builder routeBuilder = Route.newBuilder();
                                JSONObject data = json.getJSONObject("data");
                                routeBuilder
                                        .setNumber(data.getInt("Number"))
                                        .setType(data.getString("Type"))
                                        .setColor(data.getString("color"))
                                        .setCost(data.getDouble("cost"))
                                        .setDistance(data.getDouble("distance"))
                                        .setId(data.getInt("id"))
                                        .setTitle(data.getString("title"));

                                JSONArray segmentsJSON = data.getJSONArray("segments");
                                List<Segment> segments = new ArrayList<>();
                                for(int i = 0; i < segmentsJSON.length(); i++) {
                                    JSONObject segmentJSON = segmentsJSON.getJSONObject(i);
                                    Segment.Builder segmentBuilder = Segment.newBuilder()
                                            .setBuilt(segmentJSON.getInt("built"))
                                            .setDirection(segmentJSON.getInt("direction"))
                                            .setId(segmentJSON.getInt("id"))
                                            .setPosition(segmentJSON.getInt("position"))
                                            .setRouteId(segmentJSON.getInt("routeId"))
                                            .setStopping(segmentJSON.getInt("stoppingId"));

                                    JSONArray pointsJSON = segmentJSON.getJSONArray("points");
                                    List<Point> points = new ArrayList<>();
                                    for(int j = 0; j < pointsJSON.length(); j++) {
                                        JSONObject pointJSON = pointsJSON.getJSONObject(j);
                                        Point point = Point.newBuilder()
                                                .setId(pointJSON.getInt("id"))
                                                .setLat(pointJSON.getDouble("lat"))
                                                .setLng(pointJSON.getDouble("lng"))
                                                .setPosition(pointJSON.getInt("position"))
                                                .setSegmentId(pointJSON.getInt("segmentId"))
                                                .build();
                                        points.add(point);
                                    }
                                    segmentBuilder.setPoints(points);
                                    segments.add(segmentBuilder.build());
                                }
                                JSONArray transports = data.getJSONArray("transport");
                                ArrayList<Transport> transportList = new ArrayList<>();
                                for(int i = 0; i < transports.length(); i++) {
                                    JSONObject transportJSON = transports.getJSONObject(i);
                                    Transport transport = Transport.newBuilder()
                                            .setId(transportJSON.getString("id"))
                                            .setInventoryNumber(transportJSON.getString("inventoryNumber"))
                                            .setUrl(transportJSON.getString("url"))
                                            .setRouteId(transportJSON.getString("routeId"))
                                            .setTitle(transportJSON.getString("type"))
                                            .setSeats(transportJSON.getInt("seats"))
                                            .setTitle(transportJSON.getString("title"))
                                            .build();
                                    transportList.add(transport);
                                }
                                routeBuilder.setSegments(segments);
                                routeBuilder.setTransport(transportList);
                                Route route = routeBuilder.build();
                                contentProvider.setRoute(route);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "loadRouteError: " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("type", type);
                params.put("number", number);
                params.put("language", language);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                return headers;
            }
        };

        requestQueue.add(stringRequest);
    }

    public void loadStoppingList(final List<Integer> stoppings, final String language) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                urls.get("LoadingListStopping"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if(json.getBoolean("success")) {
                                JSONArray listJSON = json.getJSONArray("list");
                                List<String> stoppingList = new ArrayList<>();
                                for(int i = 0; i < listJSON.length(); i++) {
                                    stoppingList.add(listJSON.getJSONObject(i).getString("title"));
                                }
                                Log.d(TAG, "Stopping list: " + stoppingList);
                                contentProvider.setStoppingList(stoppingList);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading stopping list: " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                JSONArray stoppingsJSON = new JSONArray(stoppings);
                params.put("stopping", stoppingsJSON.toString());
                params.put("language", language);
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    public void loadMasterList(final String language) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                urls.get("LoadingListMaster"),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if(json.getBoolean("success")) {
                                JSONArray listJSON = json.getJSONArray("list");
                                List<Master> masters = new ArrayList<>();
                                for(int i = 0; i < listJSON.length(); i++) {
                                    JSONObject masterJSON = listJSON.getJSONObject(i);
                                    Master master = Master.newBuilder()
                                            .setId(masterJSON.getInt("id"))
                                            .setTitle(masterJSON.getString("title"))
                                            .build();
                                    masters.add(master);
                                }
                                contentProvider.setMasterList(masters);
                                Log.d(TAG, "Masters list: " + masters);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading master list: " + error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("language", language);
                return params;
            }
        };
        requestQueue.add(stringRequest);
    }

    public void getState(final ArrayList<String> transportKeys) {
        String url = urls.get("GetState") + "?";
        for(int i = 0; i < transportKeys.size(); i++) {
            url += "imei[]=" + transportKeys.get(i);
            if(i < transportKeys.size() - 1)
                url += "&";
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray states = new JSONArray(response);
                            ArrayList<State> stateList = new ArrayList<>();
                            for(int i = 0; i < states.length(); i++) {
                                JSONObject stateJSON = states.getJSONObject(i);
                                State state = State.newBuilder()
                                        .setLat(stateJSON.getDouble("lat"))
                                        .setLng(stateJSON.getDouble("lng"))
                                        .setTs(stateJSON.getString("ts"))
                                        .setSpeed(stateJSON.getInt("speed"))
                                        .setAzimuth(stateJSON.getInt("azimut"))
                                        .setIgnit(stateJSON.getInt("ignit"))
                                        .setGsmPower(stateJSON.getInt("gsmpower"))
                                        .setImei(stateJSON.getString("imei"))
                                        .build();
                                stateList.add(state);
                            }
                            contentProvider.setState(stateList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "getStateError: " + error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                return headers;
            }
        };

        requestQueue.add(stringRequest);
    }
}
