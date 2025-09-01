JAVAFX_PATH = javafx-sdk-17.0.16/lib
MAIN_CLASS = TrafficIntersectionSimulation
JAVAFX_VERSION = 17.0.16
JAVAFX_ARCHIVE = openjfx-$(JAVAFX_VERSION)_bin-sdk.zip
JAVAFX_URL = https://download2.gluonhq.com/openjfx/17.0.16/openjfx-17.0.16_linux-x64_bin-sdk.zip

run:
	@echo "🔨 Building and running $(MAIN_CLASS)..."
	@javac -p $(JAVAFX_PATH) -d Build --add-modules javafx.controls src/*.java
	@java -p $(JAVAFX_PATH) -cp Build --add-modules javafx.controls $(MAIN_CLASS)

clean:
	@echo "🧹 Cleaning..."
	@rm -rf Build

install:
	@echo "Downloading JavaFX $(JAVAFX_VERSION)..."
	@wget -q $(JAVAFX_URL) -O $(JAVAFX_ARCHIVE)
	@echo "Unzipping JavaFX SDK..."
	@unzip -q -o $(JAVAFX_ARCHIVE)
	@echo "JavaFX installed in ./javafx-sdk-$(JAVAFX_VERSION)"
	@rm -f $(JAVAFX_ARCHIVE)

.PHONY: run clean