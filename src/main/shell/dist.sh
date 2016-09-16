mkdir ./target/dist
mkdir ./target/dist/vader
cp ./target/*.jar ./target/dist/vader
cp ./src/main/shell/vdr.bat  ./target/dist/vader
cp ./src/main/shell/vdr.sh  ./target/dist/vader
zip -r ./target/dist/vader-0.0.3.zip ./target/dist/vader