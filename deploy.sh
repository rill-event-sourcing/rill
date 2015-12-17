eval $(gpg-agent --daemon --enable-ssh-support)
export GPG_TTY=`tty`
lein repack deploy clojars


