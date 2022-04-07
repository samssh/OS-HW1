[ -f tester.zip ] && rm tester.zip
cp -r src/test/java ./
mv ./java ./test
zip -r tester.zip ./test ./valid_files ./tester_config.json
rm -r ./test