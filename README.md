# ImageManager Library
 

## Add Your Repository Sources
```
maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/2BAYT/imagemanagernadroid")
    credentials {
        username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USER")
        password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_PERSONAL_ACCESS_TOKEN")
    }
}
```


    

## Add Dependency
```
implementation 'com.twobayt.imagemanager:core:1.0.5'
implementation 'io.reactivex.rxjava2:rxandroid:2.0.2'
```

## Crop feature
`implementation 'com.edmodo:cropper:1.0.1'`


## Usage
1. Create Manager with settings
```
 imageManager = ImageManager.Builder(context)
            .targetWidth(1600)
            .targetHeight(1800)
            .crop(true)
            .sampleSize(SampleSize.EXTRABIG)
            .build()

imageManager?.prepareInstance(savedInstanceState)
        
```

2. Register Listeners
```
imageManager?.register(requireActivity(), Fragment, ICropProvideer, { 
    onImageSelected(it) 
}, {})
```

3. Launch Camera Or Gallery
```
imageManager.launchCamera()
imageManager.launchGallery()
```


## Save Manager Instance 
`imageManager.onSaveInstanceState(outState)`