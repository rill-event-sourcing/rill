Rails.application.routes.draw do

  match 'select_course',  to: 'courses#select', via: :post
  match 'publish_course', to: 'home#publish', via: :post

  resources :chapters do
    member do
      post 'activate'
      post 'deactivate'
      post 'moveup'
      post 'movedown'
    end

    resources :sections do
      member do
        post 'activate'
        post 'deactivate'
        post 'moveup'
        post 'movedown'
      end
      resources :subsections do
        collection do
          get 'preview'
          post 'save'
        end
      end
      resources :questions do
        member do
          post 'activate'
          post 'deactivate'
          post 'moveup'
          post 'movedown'
          get 'preview'
        end
      end
    end
  end

  resources :inputs do
    resources :answers
    resources :choices
  end

  root to: 'home#index'
end
