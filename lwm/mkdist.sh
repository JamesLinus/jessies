#!/bin/sh

DISTFILES="AUTHORS BUGS COPYING ChangeLog INSTALL Imakefile README TODO client.c cursor.c disp.c error.c ewmh.c ewmh.h lwm.c lwm.h lwm.man manage.c mouse.c no_xmkmf_makefile resource.c session.c shape.c"

VERSION=`cat VERSION`
mkdir /tmp/lwm-$VERSION

for f in `echo $DISTFILES`; do
	if [ -f $f.dist ]; then
		cp $f.dist /tmp/lwm-$VERSION/$f
	else
		cp $f /tmp/lwm-$VERSION/$f
	fi
done

(cd /tmp ; tar zcvf lwm-$VERSION.tar.gz lwm-$VERSION)
scp /tmp/lwm-$VERSION.tar.gz jessies:www.jfc.org.uk/html/files/lwm/
scp ChangeLog jessies:www.jfc.org.uk/html/software/lwm-stable-ChangeLog.txt
scp lwm.1x.html jessies:www.jfc.org.uk/html/software/lwm-stable.1x.html

rm -rf /tmp/lwm-$VERSION lwm-$VERSION.tar.gz
