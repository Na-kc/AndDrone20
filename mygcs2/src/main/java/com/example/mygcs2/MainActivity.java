package com.example.mygcs2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.ArrowheadPathOverlay;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.GeometryUtils;
import com.naver.maps.map.util.MarkerIcons;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.EpmGripper;
import com.o3dr.services.android.lib.drone.mission.item.command.SetServo;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.droidplanner.services.android.impl.core.helpers.geoTools.GeoTools;
import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.helpers.geoTools.LineTools;
import org.droidplanner.services.android.impl.core.helpers.units.Area;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.example.mygcs2.Values.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        DroneListener, TowerListener, LinkListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private int DEFAULT_ZOOM_LEVEL = 17;
    LatLng DEFAULT_LATLNG = new LatLng(35.9436,126.6842);
    private static final String TAG = MainActivity.class.getSimpleName();

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private Spinner modeSelector;
    private Marker droneMarker = new Marker();
    List<LatLng> polyMarkersLatLng = new ArrayList<>();
    PolygonOverlay polygon = new PolygonOverlay();
    Marker marker_goal = new Marker(); // Guided 모드 마커

    int rNeghborInedx;
    int lNeghborInedx;

    int testcount =0 ;

    PolygonOverlay polygonOverlay = new PolygonOverlay();
    List<LatLng> latLngsBoundx2 = new ArrayList<>();

    PathOverlay path = new PathOverlay();
    PathOverlay pathOverlay = new PathOverlay();
    List<LatLng> dronePathCoords = new ArrayList<>();
    private boolean isMapLinked = false;
    private boolean isCameraLocked = true;
    private boolean isPolygonMissionEnabled = false;

    private double droneMissionAlt = 5.0;

    ArrayList<String> recycler_list = new ArrayList<>();

    List<Marker> markers = new ArrayList<>();

    private int minInedx=0;

    //미션 관련 변수
    Mission droneMission;
    List<LatLong> droneMissionCoords = new ArrayList<>();
    protected double recentAltitude = 0;
    private PathOverlay missionPathOverlay;
    private ArrayList<LatLong> missionPointLatLongList = new ArrayList<>();
    private boolean tmpgriper = false;
    EpmGripper epmGripper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemUI();

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                hideSystemUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                hideSystemUI();
            }
        });

        // ↓ map sync
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        initLayout();
    }

    public void test_btn(View view) throws Exception {
//        if(this.polyMarkersLatLng.size() < 2){
//            sendRecyclerMessage("최소 3개의 점을 입력해야 합니다.");
//        }else {
//            try
//            {
//                if(polygon.getMap()!=null){
//                    getDronePolyPath();
//                    ArrayList<LatLng> missionlatlng = new ArrayList<LatLng>();
//                    for(LatLong latlongs : this.droneMissionCoords){
//                        missionlatlng.add(MyUtil.latLongToLatLng(latlongs));
//                    }
//                    this.pathOverlay.setCoords(missionlatlng);
//                    this.pathOverlay.setMap(this.naverMap);
//                }
//            }
//            catch (Exception e)
//            {
//                Log.d("myCheck", "예외처리 : " + e.getMessage());
//            }
//        }

        EpmGripper trueEpmGripper = new EpmGripper();
        EpmGripper falseEepmGripper = new EpmGripper();
        trueEpmGripper.setRelease(true);
        falseEepmGripper.setRelease(false);
        Mission gripperMission = new Mission();
        gripperMission.addMissionItem(trueEpmGripper);
        gripperMission.addMissionItem(falseEepmGripper);

        MissionApi.getApi(this.drone).setMission(gripperMission, true);
    }

    public void btnstream(View view) {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivityForResult(intent, 1);
    }

    public void btnclearRecycler(View view) {
        clearRecyclerMessage();
    }

    private void sendRecyclerMessage(String message) {
        String localTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.recycler_list.add(String.format("[" + localTime + "]" + message));
        refreshRecyclerView();
    }

    public void clearRecyclerMessage(){
        this.recycler_list.clear();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 리사이클러뷰에 SimpleAdapter 객체 지정.
        SimpleTextAdapter adapter = new SimpleTextAdapter(recycler_list);
        recyclerView.setAdapter(adapter);

        recyclerView.scrollToPosition(recycler_list.size()-1);
        //recyclerView.smoothScrollToPosition(recycler_list.size()-1);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.HOME_UPDATED:
                //TODO 사용하지 않는 코드 (사용자와의 거리) 001
                //updateDistanceFromHome();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYAW();
                break;

            case AttributeEvent.GPS_COUNT:
                updateSatellite();
                break;

            case AttributeEvent.GPS_POSITION:
                updateDroneMarker();
                updateDronePath();
                break;

            case AttributeEvent.AUTOPILOT_MESSAGE:
                Bundle bundle = extras;
                String msg = extras.getString("com.o3dr.services.android.lib.attribute.event.extra.AUTOPILOT_MESSAGE");
                alertUser("[AUTOPILOT_MESSAGE]" + msg);
                break;

            default:
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = this.drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected())
        {
            this.drone.disconnect();
        }
        else
        {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(this.droneMissionAlt, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("고도: %3.1f", droneAltitude.getAltitude()) + "m");
        recentAltitude = droneAltitude.getAltitude();
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("속도: %3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, R.layout.spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void updateBattery(){
        TextView batteryTextView = (TextView) findViewById(R.id.batteryBalueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        batteryTextView.setText(String.format("전압: %3.1f", droneBattery.getBatteryVoltage()) + "v");
    }

    protected void updateYAW(){
        TextView droneYAWTextView = (TextView) findViewById(R.id.YAWTextView);
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double droneYaW = droneAttitude.getYaw();
        droneYAWTextView.setText(String.format("YAW: %3.1f", droneYaW) + "deg");

        this.droneMarker.setAngle((float)(droneYaW + 360));
    }

    protected void updateSatellite(){
        TextView droneSatellite = (TextView) findViewById(R.id.satelliteTextView);
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        droneSatellite.setText(String.format("위성: %d", droneGPS.getSatellitesCount()));
    }

    protected void updateDroneMarker(){
        LatLng currentLatlngLocation = getCurrentLatLng();
        this.droneMarker.setPosition(currentLatlngLocation);
        this.droneMarker.setMap(naverMap);

        if(this.isCameraLocked){
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatlngLocation);
            this.naverMap.moveCamera(cameraUpdate);
        }
    }

    protected void updateDronePath(){
        this.dronePathCoords.add(getCurrentLatLng());
        this.path.setCoords(dronePathCoords);
    }

    protected LatLong getCurrentLatLong(){
        Gps gps = this.drone.getAttribute(AttributeType.GPS);
        return gps.getPosition();
    }

    //TODO gps수신이 불량할 경우 NullPointerException발생, 이경우 default LatLong을 어떻게?? 임시로 (0,0)으로 넣어놓음
    protected LatLng getCurrentLatLng(){
        LatLng currentLatlngLocation = new LatLng(0,0);

        try {
            LatLong currentLatlongLocation = getCurrentLatLong();
            currentLatlngLocation = new LatLng(currentLatlongLocation.getLatitude(),currentLatlongLocation.getLongitude());

        }
        catch(NullPointerException e) {
            //sendRecyclerMessage("GPS 수신이 불안정 합니다.");

        }

        return currentLatlngLocation;
    }

    protected void alertUser(String message) {
        sendRecyclerMessage(message);
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                this.naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                if(isPolygonMissionEnabled) {
                    makePolygon(latLng);
                }
            }
        });

        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(int i, boolean b) {
            }
        });

        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                sendRecyclerMessage(location.getLatitude() + ", " + location.getLongitude());
            }
        });

        naverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
                                               @Override
                                               public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng coord) {
                                                   LongClickWarning(pointF, coord);
                                               }
                                           });

        initMap();
    }

    public void drawBound(PolygonOverlay polygon){
        LatLng center = polygon.getBounds().getCenter();
        Double offsetX = polygon.getBounds().getEastLongitude() - center.longitude;
        Double offsetY = polygon.getBounds().getNorthLatitude() - center.latitude;
        sendRecyclerMessage("X = "+ offsetX + " Y="+ offsetY);

        Double north = polygon.getBounds().getNorthLatitude() + offsetY;
        Double south = polygon.getBounds().getSouthLatitude() - offsetY;
        Double east = polygon.getBounds().getEastLongitude() + offsetX;
        Double west = polygon.getBounds().getWestLongitude() - offsetX;

        this.latLngsBoundx2.clear();
        this.latLngsBoundx2.add(new LatLng(north,east));//ne
        this.latLngsBoundx2.add(new LatLng(north,west));//nw
        this.latLngsBoundx2.add(new LatLng(south,west));//sw
        this.latLngsBoundx2.add(new LatLng(south,east));//se

        this.polygonOverlay.setCoords(this.latLngsBoundx2);
        this.polygonOverlay.setColor(Color.TRANSPARENT);
        this.polygonOverlay.setOutlineWidth(3);
        this.polygonOverlay.setMap(this.naverMap);
    }

    public void manageMarker(LatLng latLng, String captionText, OverlayImage markerIcons){
        Marker marker = new Marker(latLng);
        marker.setCaptionText(captionText);
        marker.setIcon(markerIcons);
        marker.setWidth(MARKER_SIZE);
        marker.setHeight(MARKER_SIZE);
        marker.setMap(naverMap);
        this.markers.add(marker);
    }

    public void manageMarker(LatLng latLng, String captionText){
        manageMarker(latLng, captionText, MarkerIcons.GREEN);
    }

    public void manageMarker(LatLong latLong, String captionText){
        manageMarker(new LatLng(latLong.getLatitude(),latLong.getLongitude()), captionText, MarkerIcons.GREEN);
    }

    public void manageMarker(LatLong latLong, String captionText, OverlayImage markerIcons){
        manageMarker(new LatLng(latLong.getLatitude(),latLong.getLongitude()), captionText, markerIcons);
    }

    public void getFirstPoint(List<LatLng> path){
        LatLng currentDronePosition = getCurrentLatLng();
        //TODO 지워야하는 임시좌표
        currentDronePosition = new LatLng(35.9436,126.6842);
        manageMarker(currentDronePosition, "드론", MarkerIcons.BLACK);

        //1-1.모든 꼭지점과 현 드론위치사이 거리 등록
        List<Double> distanceToVertexes = new ArrayList<>();
        for(LatLng latLng : polygon.getCoords()){
            distanceToVertexes.add(latLng.distanceTo(currentDronePosition));
        }
        //1-2.최소거리추출 → 시작점
        this.minInedx = distanceToVertexes.indexOf(Collections.min(distanceToVertexes));
        path.add(this.polygon.getCoords().get(minInedx));
    }

    public void getSecondPoint(List<LatLng> path){
        LatLng firstPoint = path.get(0);
        LatLng secondPoint;

        //TODO 긴변을 사용할지 다른변을 사용할지 옵션을 받아서 사용
        if(minInedx == 0){
            this.lNeghborInedx = minInedx + 1;
            this.rNeghborInedx = polygon.getCoords().size()-1;
        }
        else if(minInedx == polygon.getCoords().size()-1){
            this.lNeghborInedx = 0;
            this.rNeghborInedx = minInedx - 1;
        }
        else {
            this.lNeghborInedx = minInedx + 1;
            this.rNeghborInedx = minInedx - 1;
        }

        sendRecyclerMessage(String.format("전체%d%n현재%d%n왼쪽%d%n오른쪽%d%n",
                this.polygon.getCoords().size(), this.minInedx, this.lNeghborInedx, this.rNeghborInedx));

        double distanceToRight = firstPoint.distanceTo(this.polygon.getCoords().get(this.rNeghborInedx));
        double distanceToLeft = firstPoint.distanceTo(this.polygon.getCoords().get(this.lNeghborInedx));

        if(distanceToLeft > distanceToRight){//왼쪽이 더 긴 경우 왼쪽방향으로 시작
            secondPoint = this.polygon.getCoords().get(this.lNeghborInedx);
        }  else {
            secondPoint = this.polygon.getCoords().get(this.rNeghborInedx);
        }
        path.add(secondPoint);
    }

    public Polygon getPolygonfromPolygonOverlay(PolygonOverlay polygonOverlay){
        Polygon polygon = new Polygon();
        for(LatLng latLng: polygonOverlay.getCoords()){
            polygon.addPoint(new LatLong(latLng.latitude,latLng.longitude));
        }
        return polygon;
    }

    public LatLng getLatLngfromLatLong(LatLong latLong){
        return new LatLng(latLong.getLatitude(),latLong.getLongitude());
    }

    public LatLong getLatLongfromLatLng(LatLng latLng){
        return new LatLong(latLng.latitude,latLng.longitude);
    }

    public List<LatLong> getLatLongListfromLatLngList(List<LatLng> latLngs){
        List<LatLong> latLongs = new ArrayList<>();
        for(LatLng latLng: latLngs){
            latLongs.add(new LatLong(latLng.latitude,latLng.longitude));
        }
        return latLongs;
    }

    public List<LatLng> getLatLngListfromLatLongList(List<LatLong> latLongs){
        List<LatLng> latLngs = new ArrayList<>();
        for(LatLong latLong: latLongs){
            latLngs.add(new LatLng(latLong.getLatitude(),latLong.getLongitude()));
        }
        return latLngs;
    }

    public double getLongestDistance(List<LatLng> latLngs, LatLng reference){
        List<Double> distances = new ArrayList<>();
        for(LatLng latLng : latLngs){
            distances.add(latLng.distanceTo(reference));
        }
        double longest = Collections.max(distances);
        return longest;
    }

    public boolean isInside(LatLong latLong, Polygon polygon){
        double angle = 0;
        for(LatLong latLongPoly : polygon.getPoints()){
            //angle += angl
        }
//bool isInside(point a, polygon B)
//{
//    double angle = 0;
//    for(int i = 0; i < B.nbVertices(); i++)
//    {
//        angle += angle(B[i],a,B[i+1]);
//    }
//    return (abs(angle) > pi);
//}
        return true;
    }

    public List<LatLong> getIntersection(Polygon polygon, LineLatLong gridLine){
        List<LatLong> latLongs = new ArrayList<>();
        for(LineLatLong lineLatLong: polygon.getLines()){
            LatLong intersection = LineTools.FindLineIntersection(lineLatLong,gridLine);
            if(intersection != null){
                latLongs.add(intersection);
            }
        }
        return latLongs;
    }
/*
    public void getDronePolyPath(PolygonOverlay polygon, int distance, float angle) throws Exception {
        int startPoint = START_POINT_NEAREST;
        getDronePolyPath(polygon, distance, angle, startPoint);
    }
*/
    //TODO main
    public void getDronePolyPath() throws Exception {
        List<LatLong> polygonPoint = new ArrayList<>();
        for(LatLng latLng : this.polyMarkersLatLng) {
            polygonPoint.add(MyUtil.latLngToLatLong(latLng));
        }

        double angle = (double)TMP_ANGLE;
        double distance = (double)TMP_DISTANCE;

        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoint, angle, distance).getGrid();
        List<LineLatLong> trimedGrid = new Trimmer(circumscribedGrid, makePoly().getLines()).getTrimmedGrid();

        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if(line.getStart().getLatitude() > line.getEnd().getLatitude()) {
                LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
                trimedGrid.set(i, line1);
            }
        }

//        Gps gps = this.drone.getAttribute(AttributeType.GPS);
//        LatLong dronePosition = gps.getPosition();
//
//        double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0).getStart(), trimedGrid.get(0).getEnd(), dronePosition);
//        double dist2 = MathUtils.pointToLineDistance(trimedGrid.get(trimedGrid.size()-1).getStart(), trimedGrid.get(trimedGrid.size()-1).getEnd(), dronePosition);
//
//        if (dist2 < dist1) {
//            Collections.reverse(trimedGrid);
//            double distStart = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getStart());
//            double distEnd = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getEnd());
//            if (distStart > distEnd) {
//                for (int i = 0; i < trimedGrid.size(); i++) {
//                    LineLatLong line = trimedGrid.get(i);
//                    LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
//                    trimedGrid.set(i, line1);
//                }
//            }
//        }

        for (int i = 0; i < trimedGrid.size(); i++) {
            LineLatLong line = trimedGrid.get(i);
            if (i % 2 != 0) {
                line = new LineLatLong(line.getEnd(), line.getStart());
                trimedGrid.set(i,line);
            }
        }

        this.droneMissionCoords.clear();
        for(LineLatLong lineLatLong : trimedGrid) {
            this.droneMissionCoords.add(lineLatLong.getStart());
            this.droneMissionCoords.add(lineLatLong.getEnd());
        }
        MakeWayPoint();
    }

    protected Polygon makePoly() {
        Polygon poly = new Polygon();
        List<LatLong> latLongList = new ArrayList<>();
        for(LatLng latLng : polyMarkersLatLng) {
            latLongList.add(MyUtil.latLngToLatLong(latLng));
        }
        poly.addPoints(latLongList);
        return poly;
    }

    public void makeMapping(){
        try
        {
            if(polygon.getMap()!=null){
                getDronePolyPath();
                ArrayList<LatLng> missionlatlng = new ArrayList<LatLng>();
                for(LatLong latlongs : this.droneMissionCoords){
                    missionlatlng.add(MyUtil.latLongToLatLng(latlongs));
                }
                this.pathOverlay.setCoords(missionlatlng);
                this.pathOverlay.setMap(this.naverMap);
            }
        }
        catch (Exception e)
        {
            Log.d("myCheck", "예외처리 : " + e.getMessage());
        }
    }

    private void MakeWayPoint() {
        this.droneMission = new Mission();
        EpmGripper trueGrip = new EpmGripper();
        EpmGripper falseGrip = new EpmGripper();
        trueGrip.setRelease(true);
        falseGrip.setRelease(false);

        for (int i = 0; i < this.droneMissionCoords.size(); i++) {


            Waypoint waypoint = new Waypoint();
            waypoint.setDelay(1);

            LatLongAlt latLongAlt = new LatLongAlt(this.droneMissionCoords.get(i).getLatitude(), this.droneMissionCoords.get(i).getLongitude(), this.droneMissionAlt);
            waypoint.setCoordinate(latLongAlt);

            this.droneMission.addMissionItem(waypoint);
            /*
            if(i%2 == 0){
                droneMission.addMissionItem(trueGrip);
            }
            else{
                droneMission.addMissionItem(falseGrip);
            }
            */
        }
        MissionApi.getApi(this.drone).setMission(this.droneMission, true);
    }


    private double getAngleFromCoord(LatLng latLng1, LatLng latLng2) {
        LineLatLong lineLatLong = new LineLatLong(new LatLong(latLng1.latitude,latLng1.longitude),new LatLong(latLng2.latitude,latLng2.longitude));
        return lineLatLong.getHeading();
    }

    public void makePolygon(LatLng latLng){
        manageMarker(latLng,".");
        this.polyMarkersLatLng.add(latLng);

        if(polyMarkersLatLng.size()>= 3){
            sortLatLngClockwise(this.polyMarkersLatLng);
            this.polygon.setCoords(this.polyMarkersLatLng);
            this.polygon.setMap(this.naverMap);
        }
        sendRecyclerMessage(String.format("%d",polyMarkersLatLng.size()));
    }

    public static void sortLatLngClockwise(List<LatLng> latlngs) {
        float averageX = 0;
        float averageY = 0;

        for (LatLng latLng : latlngs) {
            averageX += latLng.latitude;
            averageY += latLng.longitude;
        }

        final float finalAverageX = averageX / latlngs.size();
        final float finalAverageY = averageY / latlngs.size();

        Comparator<LatLng> comparator = (lhs, rhs) -> {
            double lhsAngle = Math.atan2(lhs.longitude - finalAverageY, lhs.latitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.longitude - finalAverageY, rhs.latitude - finalAverageX);

            // Depending on the coordinate system, you might need to reverse these two conditions
            if (lhsAngle < rhsAngle) return -1;
            if (lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(latlngs, comparator);
    }

    public static void sortMarkerClockwise(List<Marker> markers) {
        float averageX = 0;
        float averageY = 0;

        for (Marker marker : markers) {
            averageX += marker.getPosition().latitude;
            averageY += marker.getPosition().longitude;
        }

        final float finalAverageX = averageX / markers.size();
        final float finalAverageY = averageY / markers.size();

        Comparator<Marker> comparator = (lhs, rhs) -> {
            double lhsAngle = Math.atan2(lhs.getPosition().latitude - finalAverageY, lhs.getPosition().longitude - finalAverageX);
            double rhsAngle = Math.atan2(rhs.getPosition().latitude - finalAverageY, rhs.getPosition().longitude - finalAverageX);

            // Depending on the coordinate system, you might need to reverse these two conditions
            if (lhsAngle < rhsAngle) return -1;
            if (lhsAngle > rhsAngle) return 1;

            return 0;
        };

        Collections.sort(markers, comparator);
    }

    private void LongClickWarning(@NonNull PointF pointF, @NonNull final LatLng coord) {
        MyAlertDialog builder = new MyAlertDialog(this);
        builder.setTitle("가이드 모드");
        builder.setMessage("클릭한 지점으로 이동하게 됩니다. 이동하시겠습니까?");
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 도착지 마커 생성
                marker_goal.setMap(null);
                marker_goal.setPosition(new LatLng(coord.latitude, coord.longitude));
                marker_goal.setIcon(OverlayImage.fromResource(R.drawable.target));
                marker_goal.setAnchor(new PointF(0.5f,0.5f));
                marker_goal.setWidth(70);
                marker_goal.setHeight(70);
                marker_goal.setMap(naverMap);

                // Guided 모드로 변환
                ChangeToGuidedMode();

                // 지정된 위치로 이동
                GotoTartget();
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        builder.show();

    }

    private void ChangeToGuidedMode() {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("가이드 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("가이드 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("가이드 모드 변경 실패.");
            }
        });
    }

    private void GotoTartget() {
        ControlApi.getApi(this.drone).goTo(
                new LatLong(marker_goal.getPosition().latitude, marker_goal.getPosition().longitude),
                true, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지로 향합니다.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("이동 할 수 없습니다 : " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("이동 할 수 없습니다.");
                    }
                });
    }

    private void GotoTartget(LatLong latLong) {
        ControlApi.getApi(this.drone).goTo(latLong,true, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지로 향합니다.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("이동 할 수 없습니다 : " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("이동 할 수 없습니다.");
                    }
                });
    }

    public void btn_hybrid(View view) {
        naverMap.setMapType(NaverMap.MapType.Hybrid);
    }

    public void btn_basic(View view) {
        naverMap.setMapType(NaverMap.MapType.Basic);
    }

    public void onSubOnBtnTap(View view){
        this.droneMission = new Mission();
        EpmGripper trueGrip = new EpmGripper();
        trueGrip.setRelease(true);
        droneMission.addMissionItem(trueGrip);
        MissionApi.getApi(this.drone).setMission(this.droneMission, true);
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드로 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드로 변경 실패.");
            }
        });
    }

    public void onSubOffBtnTap(View view){
        this.droneMission = new Mission();
        EpmGripper falseGrip = new EpmGripper();
        falseGrip.setRelease(false);
        droneMission.addMissionItem(falseGrip);
        MissionApi.getApi(this.drone).setMission(this.droneMission, true);
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Auto 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Auto 모드로 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Auto 모드로 변경 실패.");
            }
        });
    }

    public void initLayout(){
        initAltitudeButton();
        initDroneMarker();
        updateAltBtnVal();
    }

    public void initMap(){

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setZoomControlEnabled(false);

        naverMap.setCameraPosition(new CameraPosition(DEFAULT_LATLNG, DEFAULT_ZOOM_LEVEL));
        naverMap.setMapType(MAPTYPE_DEFAULT);

        polygon.setColor(POLYGON_COLOR);
        polygon.setOutlineColor(POLYGON_OUTLINE_COLOR);
        polygon.setOutlineWidth(POLYGON_OUTLINE_WIDTH);

    }

    public void initAltitudeButton(){
        Button altBtnAdd = findViewById(R.id.btn_current_mission_alt_add);
        Button altBtnSub = findViewById(R.id.btn_current_mission_alt_sub);

        altBtnAdd.setVisibility(Button.INVISIBLE);
        altBtnSub.setVisibility(Button.INVISIBLE);
    }

    private void initDroneMarker(){
        droneMarker.setIcon(OverlayImage.fromResource(R.drawable.drone));
        droneMarker.setAnchor(new PointF(0.5f, 0.5f));
        droneMarker.setWidth(100);
        droneMarker.setHeight(330);
        droneMarker.setFlat(true);
    }

    //TODO path값이 없을 경우 처리해야 함
    public void btnPath(View view) {
        if(isMapLinked){
            path.setMap(null);
            isMapLinked = false;
        }
        else{
            path.setMap(naverMap);
            isMapLinked = true;
        }
    }

    public void btnAlt(View view) {
        toggleAltBtnView();
    }

    public void btnAltSub(View view) {
        droneMissionAlt -= 0.5;
        updateAltBtnVal();

    }

    public void btnAltAdd(View view) {
        droneMissionAlt += 0.5;
        updateAltBtnVal();
    }

    void toggleAltBtnView(){
        Button altBtnAdd = (Button)findViewById(R.id.btn_current_mission_alt_add);
        Button altBtnSub = (Button)findViewById(R.id.btn_current_mission_alt_sub);

        if(altBtnAdd.getVisibility()==Button.INVISIBLE){
            altBtnAdd.setEnabled(true);
            altBtnSub.setEnabled(true);
            altBtnAdd.setVisibility(Button.VISIBLE);
            altBtnSub.setVisibility(Button.VISIBLE);
            animButton(altBtnAdd,R.anim.bounce);
            animButton(altBtnSub,R.anim.bounce);
        }
        else{
            animButton(altBtnAdd,R.anim.fadeout);
            animButton(altBtnSub,R.anim.fadeout);
            //TODO 버튼상태가 enabled인지 확인할수있는가? 밑에 invisible관련은 필요가없다 왜냐 fadeout하면 어차피 안보인다
            //그러나 보이지 않을뿐 클릭이 되기 때문에 enable을 false로 바꿔 주었다.
            //altBtnAdd.clearAnimation();
            //altBtnSub.clearAnimation();
            altBtnAdd.setVisibility(Button.INVISIBLE);
            altBtnSub.setVisibility(Button.INVISIBLE);
            altBtnAdd.setEnabled(false);
            altBtnSub.setEnabled(false);
        }
    }

    void updateAltBtnVal(){
        Button altBtn = (Button)findViewById(R.id.btn_current_mission_alt);
        altBtn.setText(String.format("지령 고도 = %.1fm", droneMissionAlt));
    }

    void animButton(Button button, int animationType){
        Button mButton = button;
        //Animation animation = AnimationUtils.loadAnimation(this,R.anim.bounce);
        Animation animation = AnimationUtils.loadAnimation(this,animationType);
        mButton.startAnimation(animation);
    }

    public void hideSystemUI() {
        //hide navigationbar
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                // Set the content to appear under the system bars so that the
                                // content doesn't resize when the system bars hide and show.
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                // Hide the nav bar and status bar
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }, 100);
    }

    public void toggleCameraLock(View view) {
        ImageButton imageButton = (ImageButton)findViewById(R.id.btn_lock_camera);

        if(isCameraLocked){
            imageButton.setImageResource(R.drawable.unlockedpadlock);
            sendRecyclerMessage("지도 잠금 해제");
            isCameraLocked = false;
        }
        else {
            imageButton.setImageResource(R.drawable.lockedpadlock);
            sendRecyclerMessage("지도 잠금");
            isCameraLocked = true;
        }
    }

    public void btnMissionAB(View view) {
        //todo goto뒤에 latlng좌표 줘야함
        /*
        ControlApi.getApi(this.drone).goTo(, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int executionError) {

            }

            @Override
            public void onTimeout() {

            }
        });


        ControlApi.getApi(this.drone).goTo(
                new LatLong(marker_goal.getPosition().latitude, marker_goal.getPosition().longitude),
                true, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        alertUser("목적지로 향합니다.");
                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("이동 할 수 없습니다 : " + executionError);
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("이동 할 수 없습니다.");
                    }
                });

         */
    }

    public void btnMissionPoly(View view) {
        if(isPolygonMissionEnabled){
            sendRecyclerMessage("<임시>영역 선택 해제");
            isPolygonMissionEnabled = false;
        }
        else {
            sendRecyclerMessage("<임시>터치하여 다각형 영역을 선택");
            isPolygonMissionEnabled = true;
        }
    }

    public void clear_overlay(View view) {
        polyMarkersLatLng.clear();
        polygonOverlay.setMap(null);
        polygon.setMap(null);

        for(Marker marker: markers){
            marker.setMap(null);
        }
        markers.clear();

    }
}