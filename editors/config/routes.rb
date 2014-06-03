Rails.application.routes.draw do
  resources :courses

  root to: 'home#index'
end
