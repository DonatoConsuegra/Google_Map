package com.example.google_maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private Slider sliderRadio;
    private TextView txtLat, txtLong;
    private double lat = -1.0119, lng = -79.4639; // Coordenadas iniciales de Quevedo
    private float radio = 1;
    private List<Marker> markers = new ArrayList<>();
    private Circle circulo;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate called");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Error: No se pudo encontrar el fragmento del mapa");
        }

        sliderRadio = findViewById(R.id.sliderRadio);
        txtLat = findViewById(R.id.txtLat);
        txtLong = findViewById(R.id.txtLong);

        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radio = value;
            updateInterfaz();
        });

        requestQueue = Volley.newRequestQueue(this);

        checkLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Log.d(TAG, "onMapReady called");

        // Habilita la capa de MyLocation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Configuración inicial del mapa
        LatLng quevedo = new LatLng(lat, lng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(quevedo, 13));

        // Añade un marcador para verificar si se visualiza
        mMap.addMarker(new MarkerOptions().position(quevedo).title("Quevedo"));

        mMap.setOnCameraIdleListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            lat = center.latitude;
            lng = center.longitude;
            updateInterfaz();
        });

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();
            return true;
        });

        mMap.setOnMapClickListener(latLng -> {
            Log.d(TAG, "Map clicked at: " + latLng.toString());
            Toast.makeText(this, "Mapa clickeado en: " + latLng.toString(), Toast.LENGTH_SHORT).show();
        });

        updateInterfaz();
    }

    private void updateInterfaz() {
        txtLat.setText(String.format("Lat: %.4f", lat));
        txtLong.setText(String.format("Lng: %.4f", lng));
        pintarCirculo();
        cargarLugaresTuristicos();
    }

    private void pintarCirculo() {
        if (circulo != null) {
            circulo.remove();
        }
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(lat, lng))
                .radius(radio * 1000) // Convertir a metros
                .strokeColor(Color.RED)
                .fillColor(Color.argb(50, 150, 50, 50));
        circulo = mMap.addCircle(circleOptions);
    }

    private void cargarLugaresTuristicos() {
        String url = "https://turismoquevedo.com/lugar_turistico/json_getlistadoMapa?lat=-1.04544038451566&lng=-79.48597144719564&radio=3" + lat +
                "&lng=" + lng + "&radio=" + radio;

        Log.d(TAG, "Cargando lugares turísticos desde: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (Marker marker : markers) {
                            marker.remove();
                        }
                        markers.clear();

                        JSONArray jsonLista = response.getJSONArray("data");
                        Log.d(TAG, "Número de lugares turísticos cargados: " + jsonLista.length());

                        for (int i = 0; i < jsonLista.length(); i++) {
                            JSONObject lugar = jsonLista.getJSONObject(i);
                            LatLng position = new LatLng(lugar.getDouble("lat"), lugar.getDouble("lng"));
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(lugar.getString("nombre")));
                            markers.add(marker);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al procesar JSON: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error al cargar los datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error de red: " + error.toString());
                    Toast.makeText(MainActivity.this, "Error de red: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    if (mMap != null) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_LONG).show();
            }
        }
    }
}