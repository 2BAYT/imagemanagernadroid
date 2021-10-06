# ImageManagerExample
 
## Add Dependency
`com.twobayt.imagemanager:core:1.0.0`

## Crop feature
`implementation 'com.edmodo:cropper:1.0.1'`


## Usage
1. Create Manager with settings
```
var imageManager:ImageManager = ImageManager.Builder(context)
            .crop(true)
            .sampleSize(SampleSize.BIG)
            .build()
```

2. Register Listeners
```
imageManager.registerCameraLauncher(Activity, Fragment, ICropProvider){  }
imageManager.registerGalleryLauncher(Activity, Fragment, ICropProvider){  }
```

3. Launch Camera Or Gallery
```
imageManager.launchCamera()
imageManager.launchGallery()
```


## Save Manager Instance 
`imageManager.onSaveInstanceState(outState)`