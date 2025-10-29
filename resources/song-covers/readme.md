# Rename all images in directory:

```shell
prefix="autumn"
counter=1

for file in *.jpg; do
  new_name=$(printf "%s-%03d.jpg" "$prefix" "$counter")
  mv -- "$file" "$new_name"
  ((counter++))
done
```

# Resize images:

```shell
mkdir resized
mkdir resized/hdpi
mkdir resized/xhdpi
for img in *.jpg; do
  magick "$img" -resize 480x300^ -gravity center -extent 480x300 "resized/hdpi/$img"
  magick "$img" -resize 768x480^ -gravity center -extent 768x480 "resized/xhdpi/$img"
done
```