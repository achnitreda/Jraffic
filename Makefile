JAVAFX_PATH = javafx-sdk-17.0.16/lib
MAIN_CLASS = TrafficIntersectionSimulation

run:
	@echo "🔨 Building and running $(MAIN_CLASS)..."
	@javac -p $(JAVAFX_PATH) -d Build --add-modules javafx.controls $(MAIN_CLASS).java
	@java -p $(JAVAFX_PATH) -cp Build --add-modules javafx.controls $(MAIN_CLASS)

clean:
	@echo "🧹 Cleaning..."
	@rm -rf Build

.PHONY: run clean