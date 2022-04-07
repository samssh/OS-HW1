[ -f submit.zip ] && rm submit.zip
cd src/main/java || exit 1
zip -r ../../../submit.zip ./*