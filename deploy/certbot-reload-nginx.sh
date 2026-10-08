#!/bin/sh
# Certbot deploy hook: webroot renewal updates the cert on disk, nginx keeps the old one until reload.
systemctl reload nginx
