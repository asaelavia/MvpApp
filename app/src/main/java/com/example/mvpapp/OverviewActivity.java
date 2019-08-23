package com.example.mvpapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapLabeledMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverviewActivity extends FragmentActivity {
    // permissions request code
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    // map embedded in the map fragment
    private Map map = null;
    // map fragment embedded in this activity
    private SupportMapFragment mapFragment = null;
    private ArrayList<Site> sites = null;
    private Intent intent = null;
    private ListView listView = null;
    private TextView activityHeader = null;
    private Button skipSiteButton = null;
    private Button atSiteButton = null;
    private ArrayList<String> listViewArrayList = null;
    private ArrayAdapter<String> adapter = null;
    private int nextSiteIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);
        intent = getIntent();
        String name = intent.getStringExtra("name");
        String destination = intent.getStringExtra("destination");
        sites = parseListFromDB(intent);
        checkPermissions();
    }


    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initializeMap();
                break;
        }
    }

    private void initializeMap() {

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);

        // Set up disk cache path for the map service for this application
        // It is recommended to use a path under your application folder for storing the disk cache
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator +
                        ".here-maps", "com.example.mvpapp.MapService");

        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.",
                    Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();
                        map.setLandmarksVisible(true);
                        parseListFromDB(intent);
                        // Initialize next site
                        nextSiteIndex = 0;
                        setMapCenter();
                        addRouteAndMarkers();
                        fillSiteList();
                        zoomToRoute();
                        initializeEditText();
                        skipButtonFunctionality();
                        atSiteButtonFunctionality();
                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");
                    }
                }
            });
        }
    }

    private void atSiteButtonFunctionality() {
        atSiteButton = findViewById(R.id.atSite);
        atSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
            }
        });
    }


    private void skipButtonFunctionality() {
        skipSiteButton = findViewById(R.id.skip);
        skipSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextSiteIndex++;
                if (nextSiteIndex == (sites.size())) nextSiteIndex = 0;
                activityHeader.setText(sites.get(nextSiteIndex).getSiteName());
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void initializeEditText() {
        activityHeader = findViewById(R.id.site);
        activityHeader.setText(sites.get(nextSiteIndex).getSiteName());
    }

    private void fillSiteList() {
        listView = findViewById(R.id.listView);
        listViewArrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, listViewArrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView ListItemShow = (TextView) v.findViewById(android.R.id.text1);
                if (position == nextSiteIndex) {
                    ListItemShow.setBackgroundColor(Color.BLUE);
                }else {
                    ListItemShow.setBackgroundColor(Color.WHITE);
                }
                return v;
            }
        };
        listView.setAdapter(adapter);
        for (Site site : sites) {
            listViewArrayList.add(site.getSiteName());
        }
        adapter.notifyDataSetChanged();
    }

    private void setMapCenter() {
        // Set the map center to the center route (no animation)
        double sumLat = 0;
        double sumLong = 0;
        for (Site site : sites) {
            sumLat += site.getSiteLat();
            sumLong += site.getSiteLong();
        }
        double centerLat = sumLat / sites.size();
        double centerLong = sumLong / sites.size();
        map.setCenter(new GeoCoordinate(centerLat, centerLong, 0.0),
                Map.Animation.NONE);
    }

    private ArrayList<Site> parseListFromDB(Intent intent) {
        sites = new ArrayList<Site>();
        String listOfSites = intent.getStringExtra("listOfSites");
        String[] listOfSitesParts = listOfSites.split("~");
        for (String siteName : listOfSitesParts) {
            sites.add(new Site(siteName));
        }
        String sites_coordinates = intent.getStringExtra("sites_coordinates");
        String[] sites_coordinatesParts = sites_coordinates.split("@");

        {
            int i = 0;
            for (Site site : sites) {
                String coordinates = sites_coordinatesParts[i];
                String[] coordinatesParts = coordinates.split("~");
                site.setSiteLat(Double.parseDouble(coordinatesParts[0]));
                site.setSiteLong(Double.parseDouble(coordinatesParts[1]));
                ++i;
            }
        }

        return sites;
    }

    private void addRouteAndMarkers() {
        // Declare the variable (the CoreRouter)
        CoreRouter router = new CoreRouter();
        RoutePlan routePlan = new RoutePlan();
        //TODO paint route and waypoints
        for (Site site : sites) {
            routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(site.getSiteLat(),
                    site.getSiteLong())));
            GeoCoordinate coordinate = new GeoCoordinate(site.getSiteLat(), site.getSiteLong());
            MapLabeledMarker marker = new MapLabeledMarker(coordinate);
            Image image = new Image();
            try {
                image.setImageResource(R.drawable.camera);
            } catch (IOException e) {
                e.printStackTrace();
            }
            marker.setIcon(image);
            marker.setLabelText("eng", site.getSiteName());
            marker.setFontScalingFactor(2);
            map.addMapObject(marker);
        }
        // Create the RouteOptions and set its transport mode & routing type
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);

        routePlan.setRouteOptions(routeOptions);

        // Calculate the route and zoom
        router.calculateRoute(routePlan, new RouteListener());
    }

    // Zoom to route
    private void zoomToRoute() {
        Boolean first = true;
        GeoBoundingBox geoBoundingBox = null;
        for (Site site : sites) {
            GeoCoordinate coordinate = new GeoCoordinate(site.getSiteLat(), site.getSiteLong());
            if (first) {
                geoBoundingBox = new GeoBoundingBox(coordinate, coordinate);
                first = false;
                continue;
            }
            geoBoundingBox = geoBoundingBox.merge(new GeoBoundingBox(coordinate, coordinate));

        }
        double tenPercentLat = (geoBoundingBox.getTopLeft().getLatitude() -
                geoBoundingBox.getBottomRight().getLatitude()) / 10;
        double tenPercentLong = (geoBoundingBox.getBottomRight().getLongitude() -
                geoBoundingBox.getTopLeft().getLongitude()) / 10;

        double topLeftLat = geoBoundingBox.getTopLeft().getLatitude() + tenPercentLat;
        double topLeftLong = geoBoundingBox.getTopLeft().getLongitude() - tenPercentLong;
        double bottomRightLat = geoBoundingBox.getBottomRight().getLatitude() - tenPercentLat;
        double bottomRightLong = geoBoundingBox.getBottomRight().getLongitude() + tenPercentLong;
        GeoCoordinate topLeft = new GeoCoordinate(topLeftLat, topLeftLong);
        GeoCoordinate bottomRight = new GeoCoordinate(bottomRightLat, bottomRightLong);
        geoBoundingBox = new GeoBoundingBox(topLeft, bottomRight);
        map.zoomTo(geoBoundingBox, Map.Animation.NONE, 0);
    }

    private class RouteListener implements CoreRouter.Listener {

        private MapRoute mapRoute;

        // Method defined in Listener
        public void onProgress(int percentage) {
            // Display a message indicating calculation progress
        }

        // Method defined in Listener
        public void onCalculateRouteFinished(List<RouteResult> routeResult, RoutingError error) {
            // TODO If the route was calculated successfully
            if (error == RoutingError.NONE) {
                // Render the route on the map
                mapRoute = new MapRoute(routeResult.get(0).getRoute());
                map.addMapObject(mapRoute);
            } else {
                //TODO Display a message indicating route calculation failure
            }
        }
    }


}
