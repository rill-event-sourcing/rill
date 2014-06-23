def pp(value = '', type = 'debug', debug_start = false)
  start = Time.now
  logger ||= ::Rails.logger
  logger.send(type, "\e[46;1;31m")
  if @debug_date_time
    txt = "===> ∂: #{ (start - @debug_date_time).round(2) } #{ value.inspect } ===="
    p txt if Rails.env != 'production'
    logger.send(type, txt)
  else
    txt = "===> #{ start.strftime("%H:%M:%S") }.#{ start.tv_sec } ============================================================="
    logger.send(type, txt)
    logger.send(type, "\e[1;37m")
    logger.send(type, "===> #{ value.inspect }")
    logger.send(type, "\e[1;31m")
    logger.send(type, "===> =================================================================================")
  end
  logger.send(type, "\e[44;0;30m");
  @debug_date_time = start if debug_start
end
