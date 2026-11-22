-- Clear the source branding on the bundled meal template in existing catalogs.
UPDATE catalog_meal_templates
SET credited_author = ''
WHERE id = '5e2d7928985ffca18ae8fb26'
  AND credited_author <> '';
