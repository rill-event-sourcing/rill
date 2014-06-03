Rails.application.routes.draw do
  resources :courses do
    collection do
      post 'select'
    end
  end

  root to: 'home#index'
end
