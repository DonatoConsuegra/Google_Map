package com.example.google_maps;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.google_maps.R;
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

    private GoogleMap mMap;
    private Slider sliderRadio;
    private TextView txtLat, txtLong;
    private double lat, lng;
    private float radio = 1;
    private List<Marker> markers = new ArrayList<>();
    private Circle circulo;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        sliderRadio = findViewById(R.id.sliderRadio);
        txtLat = findViewById(R.id.txtLat);
        txtLong = findViewById(R.id.txtLong);

        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radio = value;
            updateInterfaz();
        });

        requestQueue = Volley.newRequestQueue(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Configuración inicial del mapa
        LatLng quevedo = new LatLng(-1.0119, -79.4639); // Coordenadas de Quevedo
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(quevedo, 13));

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
        String url = "https://turismoquevedo.com/lugar_turistico/json_getlistadoMapa?lat=" + lat +
                "&lng=" + lng + "&radio=" + radio;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (Marker marker : markers) {
                            marker.remove();
                        }
                        markers.clear();

                        JSONArray jsonLista = response.getJSONArray("data");
                        for (int i = 0; i < jsonLista.length(); i++) {
                            JSONObject lugar = jsonLista.getJSONObject(i);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lugar.getDouble("lat"), lugar.getDouble("lng")))
                                    .title(lugar.getString("nombre")));
                            markers.add(marker);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error al cargar los datos", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(jsonObjectRequest);
    }
}