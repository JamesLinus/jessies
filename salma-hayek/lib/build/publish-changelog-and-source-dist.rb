#!/usr/bin/ruby -w

# This is now performed by cron as software@jessies.org:
# @hourly /home/software/checked-out/salma-hayek/lib/build/publish-changelog-and-source-dist.rb

require "pathname"
salma_hayek = Pathname.new(__FILE__).realpath().dirname().dirname().dirname()
failed = false
Dir.glob("#{salma_hayek}/../*/www/.svn").each() {
    |svnDirectory|
    repository = Pathname.new(svnDirectory).realpath().dirname().dirname()
    Dir.chdir(repository) {
        # "At revision 2962" is uninteresting.
        updateResult = `{ umask 0002 && svn update && make publish-changelog source-dist; } 2>&1`
        if $? != 0
            $stderr.puts("Failed in #{repository}:")
            $stderr.puts(updateResult)
            failed = true
        end
    }
}
if failed
    exit(1)
end
