# What is Elephant?
Elephant is an annotation processor to apply Memoization to Utility classes.

# What is Memoization?
Memoization is a simple technique that speeds up functions by caching the results. 
To take better advantage of this technique, the result _should depend_ on input 
parameters, and the time to process the result should take longer than just the 
time spent retrieving it.

# How do I use it?
Annotate your Utilities class with the `@Elephant` annotation, and then every 
`public` and `static` function you want to memoize with `@Memoize`. That simple.

````java
@Elephant
public class SimpleUtils{

    @Memoize
    public static Integer search(String fullText, String textToSearch){
        Pattern pattern = Pattern.compile(textToSearch);
        Matcher matcher = pattern.matcher(fullText.toLowerCase());
        int counter = 0;

        while (matcher.find()) {
            counter++;
        }

        return counter;
    }
    
    public static Integer someProcess(String text){
        return text.length();
    }
}
````

After building your project, the annotation processor will generate a class aptly 
named `Elephant+[Name of the class]` (i.e. ElephantSimpleUtils). You can then 
freely use it anywhere in your project and it will automatically memorize the 
results for every call.

For the methods not marked with the `@Memoize` annotation, it will keep calling 
the original method on the Utils class. I recommend building your project every 
time you make a modification to the original class.

# Limitations _(for now)_

* Only supports Java 8 and up
* Doesn't support nested classes
* Support only for wrapper classes (Integer, Float, Double, etc)

# Android 

In case you want to use it in your Android project, you have to enable 
`jackOptions` and make sure you're working with Java 8 (It may take a while to 
build the Elephant class).

````gradle
android {
    ...
    defaultConfig {
      ...
      jackOptions{
          enabled true
      }
    }
    buildTypes {
      ...
    }
    compileOptions{
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
````

# Download

````gradle
dependencies{
  compile 'co.herovitamin.elephant:elephant:0.5'
  annotationProcessor 'co.herovitamin.elephant:elephant-processor:0.5.1'
}
````

This project is currently in development. You are more than welcome to contribute
and make it better.

# License 
````
Copyright 2017 KERRY PEREZ HUANCA

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
````
