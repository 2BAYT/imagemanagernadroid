# ImageManagerExample
 
## Add Dependency
`com.twobayt.imagemanager:core:1.0.0`

## Crop feature
`implementation 'com.edmodo:cropper:1.0.1'`


## Usage
Create Manager with settings
```
var imageManager:ImageManager = ImageManager.Builder(context)
            .crop(true)
            .sampleSize(SampleSize.BIG)
            .build()
```


## Save Manager Instance 
`imageManager.onSaveInstanceState(outState)`