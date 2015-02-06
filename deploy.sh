eval $(gpg-agent --daemon --enable-ssh-support)
export GPG_TTY=`tty`
lein deploy clojars
cd rill-psql && lein deploy clojars

