# Not the most updated. We have a separate repo for algorithm (let us know if you want access)

# Simulator Set Up (JavaFX on IntelliJ)

1. Download and extract [JavaFX SDK](https://gluonhq.com/products/javafx/).
2. Go to File -> Project Structure -> Libraries (on the left).
3. Click on the "+" button and select Java.
4. Select the `lib` folder in the downloaded JavaFX folder. 
5. Click OK twice. The `Project Structure` window should have closed.
6. Open the GUI class (`SimulatorUI.java`) and run it. You should get an error.
7. Go to Run -> Edit Configurations.
8. Click Modify Options -> Add VM Options.
9. Under the `VM Options` field, add:

```
--module-path "PATH_TO_JAVAFX_LIB_FOLDER" 
--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base
```

10. Replace `PATH_TO_JAVAFX_LIB_FOLDER` accordingly.
11. Click OK.
12. Try running the class again. It should work now.
