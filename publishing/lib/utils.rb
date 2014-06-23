def pp(value = '', soort = 'debug', debug_start = false)
  start = Time.now
  logger ||= ::Rails.logger
  logger.send(soort, "\e[46;1;31m")
  if @debug_date_time
    txt = "===> ∂: #{ (start - @debug_date_time).round(2) } #{ value.inspect } ===="
    p txt if Rails.env != 'production'
    logger.send(soort, txt)
  else
    txt = "===> #{ start.strftime("%H:%M:%S") }.#{ start.tv_sec } ============================================================="
    logger.send(soort, txt)
    logger.send(soort, "\e[1;37m")
    logger.send(soort, "===> #{ value.inspect }")
    logger.send(soort, "\e[1;31m")
    logger.send(soort, "===> =================================================================================")
  end
  logger.send(soort, "\e[44;0;30m");
  @debug_date_time = start if debug_start
end
