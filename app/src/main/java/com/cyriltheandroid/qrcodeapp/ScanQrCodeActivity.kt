package com.cyriltheandroid.qrcodeapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class ScanQrCodeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "QrCodeActivity"
        const val QR_CODE_VALUE = "qr_code_value"
        const val REQUEST_CAMERA_PERMISSION = 1
    }

    private lateinit var cameraSurfaceView: SurfaceView
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr_code)

        cameraSurfaceView = findViewById(R.id.camera_surface_view)
    }

    override fun onPause() {
        super.onPause()
        cameraSource.release()
    }

    override fun onResume() {
        super.onResume()
        initBarcodeDetector()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (isCameraPermissionGranted(requestCode, grantResults)) {
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
            overridePendingTransition(0, 0)
        } else {
            Toast.makeText(this, "Utilisation de la caméra non autorisée.", Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    private fun isCameraPermissionGranted(requestCode: Int, grantResults: IntArray) =
        requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

    private fun initBarcodeDetector() {
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        initCameraSource()
        initCameraSurfaceView()

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Log.d(TAG, "Camera has been released.")
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems

                if (barcodes.isNotEmpty()) {
                    barcodes.forEach { _, value ->
                        if (value.displayValue.isNotEmpty()) {
                            onQrCodeScanned(value.displayValue)
                        }
                    }
                }
            }
        })
    }

    private fun initCameraSource() {
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()
    }

    private fun initCameraSurfaceView() {
        cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@ScanQrCodeActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        cameraSource.start(cameraSurfaceView.holder)
                    } else {
                        ActivityCompat.requestPermissions(
                            this@ScanQrCodeActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            REQUEST_CAMERA_PERMISSION
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.release()
            }
        })
    }

    private fun onQrCodeScanned(qrCodeValue: String) {
        val intentResult = Intent()
        intentResult.putExtra(QR_CODE_VALUE, qrCodeValue)
        setResult(RESULT_OK, intentResult)
        finish()
    }
}